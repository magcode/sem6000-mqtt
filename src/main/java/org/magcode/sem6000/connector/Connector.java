package org.magcode.sem6000.connector;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
	private BlockingQueue<Command> workQueue = null;
	private ExecutorService execService = null;
	private ScheduledExecutorService scheduledExecService = null;
	private BluetoothGattCharacteristic notifyChar;
	private BluetoothDevice sem6000;
	private String mac;
	private String id;
	private String pin;
	private String notifyCharPath = "undefined";
	private int updateSeconds;
	private NotificationReceiver receiver;
	private ScheduledFuture<?> measurePublisherFuture;
	private DeviceManager manager;

	public Connector(DeviceManager manager, String mac, String pin, String id, int updateSeconds,
			NotificationReceiver receiver) {
		this.mac = mac;
		this.id = id;
		this.pin = pin;
		this.updateSeconds = updateSeconds;
		this.manager = manager;
		this.receiver = receiver;
		this.init();
	}

	private void init() {
		try {
			sem6000 = getDevice(mac);
			if (sem6000 != null) {
				if (this.connect()) {
					for (int i = 0; i < 10; i++) {
						boolean res = sem6000.isServicesResolved();
						if (res) {
							break;
						}
						logger.info("[{}] Services not yet resolved. Waiting ...", this.getId());
						Thread.sleep(1000);
					}
					logger.info("[{}] Got the service", this.getId());
					BluetoothGattService sensorService = getService(sem6000, UUID_SERVICE);
					if (sensorService != null) {
						BluetoothGattCharacteristic writeChar = sensorService.getGattCharacteristicByUuid(UUID_WRITE);
						notifyChar = sensorService.getGattCharacteristicByUuid(UUID_NOTIFY);
						notifyChar.startNotify();

						workQueue = new LinkedBlockingQueue<Command>(10);
						execService = Executors.newFixedThreadPool(1, new SendReceiveThreadFactory(this.getId()));

						Sender worker = new Sender(workQueue, writeChar, receiver, this.getId());
						execService.submit(worker);
						this.notifyCharPath = notifyChar.getDbusPath();

						Thread.sleep(500);
						workQueue.put(new LoginCommand(pin));
						Thread.sleep(1000);
						workQueue.put(new SyncTimeCommand());
						this.enableRegularUpdates();
					} else {
						logger.error("[{}] Could not get BluetoothGattService", this.getId());
					}
				}
			}
		} catch (InterruptedException e) {
			logger.error("[{}] Could not connect to device.", this.getId(), e);
		} catch (DBusException e) {
			logger.error("[{}] DBusException.", this.getId(), e);
		}
	}

	public String getNotifyCharPath() {
		return this.notifyCharPath;
	}

	public void enableRegularUpdates() {
		if (this.updateSeconds > 0) {
			if (scheduledExecService == null) {
				scheduledExecService = Executors.newScheduledThreadPool(1,
						new RequestMeasurementThreadFactory(this.getId()));
			}
			Runnable measurePublisher = new MeasurePublisher(this);
			measurePublisherFuture = scheduledExecService.scheduleAtFixedRate(measurePublisher, 5, this.updateSeconds,
					TimeUnit.SECONDS);
		}
	}

	public String getId() {
		return this.id;
	}

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
			logger.error("[{}] Could not connect", this.getId(), e);
		}
		return false;
	}

	private void disconnect() {
		try {
			if (sem6000 != null && sem6000.isConnected()) {
				logger.debug("[{}] Trying to disconnect", this.getId());
				sem6000.disconnect();
			}
		} catch (Exception e) {
			logger.error("[{}] Could not disconnect", this.getId(), e);
		}
	}

	private synchronized void ensureConnection() {
		if (sem6000 != null && !sem6000.isConnected()) {
			logger.info("[{}] Not connected at the moment.", this.getId());
			this.stop();
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				//
			}
			this.init();
		}
	}

	public void send(Command command) {
		logger.debug("[{}] Got command {}", this.getId(), ByteUtils.byteArrayToHex(command.getMessage()));
		this.ensureConnection();
		try {
			workQueue.put(command);
		} catch (InterruptedException e) {
			logger.error("Could not put command to queue", e);
		}
	}

	public void stop() {
		logger.debug("[{}] Stopping ...", this.getId());
		if (measurePublisherFuture != null) {
			measurePublisherFuture.cancel(true);
		}

		try {

			if (this.execService != null) {
				this.execService.shutdownNow();
				if (!execService.awaitTermination(100, TimeUnit.MICROSECONDS)) {
					logger.trace("Still waiting for termination ...");
				}
			}
		} catch (InterruptedException e) {
			logger.warn("Could not terminate execService.");
		}
		try {
			this.disconnect();
		} catch (Exception e) {
			logger.warn("Error during stopping.", e);
		}
		logger.debug("[{}] Stopped.", this.getId());
	}

	private BluetoothDevice getDevice(String address) throws InterruptedException {
		logger.info("[{}] Searching {} ...", this.getId(), address);
		List<BluetoothDevice> list = manager.getDevices();
		if (list == null)
			return null;

		for (BluetoothDevice oneDevice : list) {
			logger.trace("[{}] Checking device: {} with mac {}", this.getId(), oneDevice.getName(),
					oneDevice.getAddress());
			if (oneDevice.getAddress().equals(address)) {
				logger.debug("[{}] Found device {}", this.getId(), oneDevice.getName());
				return oneDevice;
			}
		}
		logger.warn("Could not find {}", address);
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
}

class MeasurePublisher implements Runnable {
	private Connector connector;
	private static Logger logger = LogManager.getLogger(MeasurePublisher.class);

	public MeasurePublisher(Connector connector) {
		this.connector = connector;
	}

	@Override
	public void run() {
		logger.trace("Sending my commands...");
		connector.send(new MeasureCommand());
		connector.send(new DataDayCommand());
	}
}

class SendReceiveThreadFactory implements ThreadFactory {
	private String id;

	public SendReceiveThreadFactory(String id) {
		this.id = id;
	}

	public Thread newThread(Runnable r) {
		return new Thread(r, "Sender-" + this.id);
	}
}

class RequestMeasurementThreadFactory implements ThreadFactory {
	private String id;

	public RequestMeasurementThreadFactory(String id) {
		this.id = id;
	}

	public Thread newThread(Runnable r) {
		return new Thread(r, "RequestMeasurement-" + this.id);
	}
}