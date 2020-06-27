package org.magcode.sem6000.connector;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freedesktop.dbus.exceptions.DBusException;
import org.magcode.sem6000.connector.send.Command;
import org.magcode.sem6000.connector.send.DataDayCommand;
import org.magcode.sem6000.connector.send.LoginCommand;
import org.magcode.sem6000.connector.send.MeasureCommand;
import org.magcode.sem6000.connector.send.SyncTimeCommand;

import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattService;

public class Connector {
	private static Logger logger = LogManager.getLogger(Connector.class);
	public static final String UUID_NOTIFY = "0000fff4-0000-1000-8000-00805f9b34fb";
	public static final String UUID_WRITE = "0000fff3-0000-1000-8000-00805f9b34fb";
	public static final String UUID_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb";
	private static final int reconnectTime = 5;
	private BluetoothGattCharacteristic writeChar;
	private BluetoothDevice sem6000;
	private String mac;
	private String id;
	private String pin;
	private String notifyCharPath = "undefined";
	private int updateSeconds;
	private boolean reconnecting;
	private int reconnectCount;
	private int reconnectsSinceLastSuccess;
	private int consecutiveReconnectLimit;
	private ScheduledExecutorService reconnectScheduler = null;
	private DeviceManager manager;

	public Connector(DeviceManager manager, String mac, String pin, String id, int updateSeconds,
			int consecutiveReconnectLimit) {
		this.mac = mac;
		this.id = id;
		this.pin = pin;
		this.updateSeconds = updateSeconds;
		this.manager = manager;
		this.consecutiveReconnectLimit = consecutiveReconnectLimit;
		reconnectScheduler = Executors.newScheduledThreadPool(1, new NamedThreadFactory(id, "Reconnect"));
		this.enableRegularUpdates();
		this.init();
	}

	private void init() {
		try {
			sem6000 = getDevice(mac);
			if (sem6000 == null) {
				logger.warn("[{}] Could not find device with this mac {}", this.getId(), this.mac);
				this.scheduleReconnect();
				return;
			}

			if (!this.connect()) {
				this.scheduleReconnect();
				return;
			}

			logger.debug("[{}] connected", this.getId());
			for (int i = 0; i < 10; i++) {
				boolean res = sem6000.isServicesResolved();
				if (res) {
					break;
				}
				logger.debug("[{}] Services not yet resolved. Waiting ...", this.getId());
				Thread.sleep(1000);
			}

			if (!sem6000.isServicesResolved()) {
				logger.debug("[{}] Could not resolve services", this.getId());
				this.scheduleReconnect();
				return;
			}

			BluetoothGattService sensorService;
			sensorService = getService(sem6000, UUID_SERVICE);
			if (sensorService == null) {
				logger.debug("[{}] Could not get BluetoothGattService", this.getId());
				scheduleReconnect();
				return;
			}

			logger.debug("[{}] Got the BluetoothGattService", this.getId());

			writeChar = sensorService.getGattCharacteristicByUuid(UUID_WRITE);
			if (writeChar == null) {
				logger.debug("[{}] Could not get 'write' characteristic", this.getId());
				scheduleReconnect();
				return;
			}

			logger.debug("[{}] Got the 'write' characteristic", this.getId());

			BluetoothGattCharacteristic notifyChar = sensorService.getGattCharacteristicByUuid(UUID_NOTIFY);
			if (notifyChar == null) {
				logger.debug("[{}] Could not get 'notify' characteristic", this.getId());
				scheduleReconnect();
				return;
			}

			logger.debug("[{}] Got the 'notify' characteristic", this.getId());

			notifyChar.startNotify();
			this.notifyCharPath = notifyChar.getDbusPath();

			Thread.sleep(500);
			setReconnecting(false);
			logger.info("[{}] Connection process successful", this.getId());
			// start with login and sync time
			this.send(new LoginCommand(pin));
			this.send(new SyncTimeCommand());
			reconnectsSinceLastSuccess = 0;
		} catch (Exception ie) {
			logger.debug("[{}] Exception during init.", this.getId(), ie);
			this.scheduleReconnect();
			return;
		}
	}

	public void enableRegularUpdates() {
		if (this.updateSeconds > 0) {
			ScheduledThreadPoolExecutor measureScheduler = new ScheduledThreadPoolExecutor(1,
					new NamedThreadFactory(this.getId(), "RequestMeasurement"));
			measureScheduler.setRemoveOnCancelPolicy(true);
			measureScheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
			measureScheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
			Runnable r = new Runnable() {
				@Override
				public void run() {
					send(new MeasureCommand());
					send(new DataDayCommand());
				}
			};
			measureScheduler.scheduleAtFixedRate(r, 5, this.updateSeconds, TimeUnit.SECONDS);
		}
	}

	/**
	 * Will attemt to connect to the BLE device
	 * 
	 * @return true for successful
	 */
	private boolean connect() {
		try {
			if (sem6000 != null && sem6000.isConnected()) {
				logger.debug("[{}] Already connected", this.getId());
				return true;
			}

			if (sem6000 != null && !sem6000.isConnected()) {
				logger.debug("[{}] Trying to connect", this.getId());
				return sem6000.connect();
			}
		} catch (Exception e) {
			logger.debug("[{}] Could not connect. ({})", this.getId(), e.getMessage());
		}
		return false;
	}

	/**
	 * Will schedule a reconnect in {@value #reconnectTime} minutes. In case a
	 * threshold for subsequent reconnect failures is reaches, this won't do
	 * anything.
	 */
	private void scheduleReconnect() {
		this.stop();
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			//
		}
		if (reconnectsSinceLastSuccess > consecutiveReconnectLimit) {
			logger.error(
					"Reached {} consecutive reconnects. Stopping reconnecting now. Make sure the device is reachable and restart the service.",
					reconnectsSinceLastSuccess);
			return;
		}
		reconnectCount++;
		reconnectsSinceLastSuccess++;
		logger.info("[{}] Scheduling reconnect #{} (#{} after last success) at {}.", this.getId(), reconnectCount,
				reconnectsSinceLastSuccess, LocalDateTime.now().plusMinutes(reconnectTime).withNano(0));
		setReconnecting(true);

		reconnectScheduler.schedule(new Runnable() {
			@Override
			public void run() {
				logger.info("[{}] Reconnect attempt", getId());
				init();
			}
		}, reconnectTime, TimeUnit.MINUTES);
	}

	/**
	 * Verifies the BLE device is connected. Returns false if reconnecting at the
	 * moment. In case it is not connected, calls scheduleReconnect() and returns
	 * false.
	 * 
	 * @return
	 */
	private synchronized boolean ensureConnected() {
		if (reconnecting) {
			logger.trace("[{}] Not accepting commands. Reconnecting at the moment.", this.getId());
			return false;
		}

		if (sem6000 != null && sem6000.isConnected() != null && !sem6000.isConnected()) {
			logger.debug("[{}] Not connected at the moment.", this.getId());
			this.scheduleReconnect();
			return false;
		}
		return true;
	}

	/**
	 * Convenience method to request all measurements in one go. Results will be
	 * reported by the ConnectionManager.
	 */
	public void requestMeasurements() {
		this.send(new MeasureCommand());
		this.send(new DataDayCommand());
	}

	public synchronized void send(Command command) {
		if (ensureConnected() && this.writeChar != null) {
			logger.debug("[{}] Sending command {}", this.getId(), ByteUtils.byteArrayToHex(command.getMessage()));
			try {
				this.writeChar.writeValue(command.getMessage(), null);
				Thread.sleep(400);
			} catch (DBusException e) {
				logger.warn("Error during sending.", e);
			} catch (InterruptedException e) {
				//
			}
		}
	}

	/**
	 * Disconnects from the device.
	 */
	public void stop() {
		logger.debug("[{}] Stopping ...", this.getId());
		try {
			if (sem6000 != null && sem6000.isConnected() != null && sem6000.isConnected()) {
				logger.debug("[{}] Trying to disconnect", this.getId());
				sem6000.disconnect();
			}
		} catch (Exception e) {
			logger.debug("[{}] Error during stopping.", this.getId());
		}
		logger.debug("[{}] Stopped.", this.getId());
	}

	private BluetoothDevice getDevice(String address) {
		logger.debug("[{}] Searching {} ...", this.getId(), address);
		List<BluetoothDevice> list = manager.getDevices();
		if (list == null)
			return null;

		for (BluetoothDevice oneDevice : list) {
			if (oneDevice.getAddress() != null) {
				logger.trace("[{}] Checking device: {} with mac {}", this.getId(), oneDevice.getName(),
						oneDevice.getAddress());
				if (oneDevice.getAddress().equals(address)) {
					logger.debug("[{}] Found device {}", this.getId(), oneDevice.getName());
					return oneDevice;
				}
			}
		}
		logger.debug("Could not find {}", address);
		return null;
	}

	private BluetoothGattService getService(BluetoothDevice device, String UUID) throws InterruptedException {
		BluetoothGattService service = null;
		List<BluetoothGattService> gatts = device.getGattServices();

		for (BluetoothGattService bluetoothGattService : gatts) {
			if (UUID_SERVICE.equals(bluetoothGattService.getUuid())) {
				service = bluetoothGattService;
			}
		}
		return service;
	}

	public String getId() {
		return this.id;
	}

	public String getNotifyCharPath() {
		return this.notifyCharPath;
	}

	private void setReconnecting(boolean rec) {
		this.reconnecting = rec;
	}
}

class NamedThreadFactory implements ThreadFactory {
	private String id;
	private String prefix;

	public NamedThreadFactory(String id, String prefix) {
		this.id = id;
		this.prefix = prefix;
	}

	public Thread newThread(Runnable r) {
		return new Thread(r, this.prefix + "-" + this.id);
	}
}