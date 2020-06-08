package org.magcode.sem6000.connector;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
import org.magcode.sem6000.NotificatioReceiver;
import org.magcode.sem6000.connector.send.Command;
import org.magcode.sem6000.connector.send.DataDayCommand;
import org.magcode.sem6000.connector.send.LoginCommand;
import org.magcode.sem6000.connector.send.MeasureCommand;
import org.magcode.sem6000.connector.send.SyncTimeCommand;

import tinyb.BluetoothDevice;
import tinyb.BluetoothException;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;
import tinyb.BluetoothManager;
import tinyb.BluetoothNotification;

public class Connector {
	private static Logger logger = LogManager.getLogger(Connector.class);

	public static final String UUID_NOTIFY = "0000fff4-0000-1000-8000-00805f9b34fb";
	public static final String UUID_WRITE = "0000fff3-0000-1000-8000-00805f9b34fb";
	public static final String UUID_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb";
	private BlockingQueue<Command> workQueue = null;
	private ExecutorService execService = null;
	private ScheduledExecutorService scheduledExecService = null;
	private BluetoothGattCharacteristic notifyChar;
	BluetoothDevice sem6000;

	public Connector(String mac, String pin, boolean enableRegularUpdates, NotificatioReceiver receiver) {
		try {
			// BluetoothManager manager = BluetoothManager.getBluetoothManager();
			// boolean discoveryStarted = manager.startDiscovery();
			sem6000 = getDevice(mac);
			if (sem6000 != null) {
				sem6000.enableConnectedNotifications(new BLEConnectedNotification());
				if (this.connect()) {
					// wait for characteristics scan
					LocalDateTime startTime = LocalDateTime.now();
					while (LocalDateTime.now().isBefore(startTime.plusSeconds(30))) {
						if (sem6000.getServicesResolved()) {
							logger.debug("[{}] Got characteristics after {} ms", sem6000.getName(),
									ChronoUnit.MILLIS.between(startTime, LocalDateTime.now()));
							break;
						}
						try {
							Thread.sleep(100);
						} catch (InterruptedException ie) {

						}
					}

					BluetoothGattService sensorService = getService(sem6000, UUID_SERVICE);

					BluetoothGattCharacteristic writeChar = getCharacteristic(sensorService, UUID_WRITE);
					notifyChar = getCharacteristic(sensorService, UUID_NOTIFY);

					workQueue = new LinkedBlockingQueue<Command>(10);
					execService = Executors.newFixedThreadPool(1);

					SendReceiveThread worker = new SendReceiveThread(workQueue, writeChar, receiver, this);
					notifyChar.enableValueNotifications(worker);

					execService.submit(worker);

					Thread.sleep(500);
					workQueue.put(new LoginCommand(pin));
					Thread.sleep(500);
					workQueue.put(new SyncTimeCommand());
					if (enableRegularUpdates) {
						this.enableRegularUpdates();
					}
				}
			}
		} catch (InterruptedException e) {
			logger.error("[{}] Could not connect to device.", sem6000.getName(), e);
		}
	}

	public void enableRegularUpdates() {
		scheduledExecService = Executors.newScheduledThreadPool(1);
		Runnable measurePublisher = new MeasurePublisher(this);

		ScheduledFuture<?> measurePublisherFuture = scheduledExecService.scheduleAtFixedRate(measurePublisher, 2, 10,
				TimeUnit.SECONDS);
	}

	public String getName() {
		return sem6000.getName();
	}

	private boolean connect() {
		logger.debug("[{}] Trying to connect", sem6000.getName());
		try {
			return sem6000.connect();
		} catch (BluetoothException e) {
			logger.error("[{}] Could not connect", sem6000.getName(), e);
		}
		return false;
	}

	private void disconnect() {
		logger.debug("[{}] Trying to disconnect", sem6000.getName());
		try {
			sem6000.disconnect();
		} catch (BluetoothException e) {
			logger.error("[{}] Could not connect", sem6000.getName(), e);
		}
	}

	public void send(Command command) {
		try {
			workQueue.put(command);
		} catch (InterruptedException e) {
			logger.error("Could not put command to queue", e);
		}
	}

	public void stop() {
		try {
			if (notifyChar != null) {
				notifyChar.disableValueNotifications();
				Thread.sleep(500);
			}
			this.disconnect();
			Thread.sleep(500);
			if (this.sem6000 != null) {
				this.sem6000.disableConnectedNotifications();
				Thread.sleep(500);
			}
			// this.sem6000.remove();
			// Thread.sleep(500);
			if (this.execService != null) {
				this.execService.shutdownNow();
				if (!execService.awaitTermination(100, TimeUnit.MICROSECONDS)) {
					logger.info("Still waiting for termination ...");
				}
			}
			if (this.scheduledExecService != null) {
				this.scheduledExecService.shutdownNow();
				if (!scheduledExecService.awaitTermination(100, TimeUnit.MICROSECONDS)) {
					logger.info("Still waiting for termination ...");
				}
			}
		} catch (InterruptedException e) {
			logger.error("Could not terminate.", e);
		}
	}

	private BluetoothDevice getDevice(String address) throws InterruptedException {
		BluetoothManager manager = BluetoothManager.getBluetoothManager();
		for (int i = 0; (i < 15); ++i) {
			logger.info("Searching {} ...", address);
			List<BluetoothDevice> list = manager.getDevices();
			if (list == null)
				return null;

			for (BluetoothDevice device : list) {
				logger.trace("Checking device: {} with mac {}", device.getName(), device.getAddress());
				if (device.getAddress().equals(address)) {
					logger.debug("Found device {}", device.getName());
					return device;
				}
			}
			Thread.sleep(2000);
		}
		logger.warn("Could not find {}", address);
		return null;
	}

	private BluetoothGattService getService(BluetoothDevice device, String UUID) throws InterruptedException {

		BluetoothGattService tempService = null;
		List<BluetoothGattService> bluetoothServices = null;
		do {
			bluetoothServices = device.getServices();
			if (bluetoothServices == null)
				return null;

			for (BluetoothGattService service : bluetoothServices) {
				logger.trace("[{}] Services exposed by device: {}", sem6000.getName(), service.getUUID());
				if (service.getUUID().equals(UUID))
					tempService = service;
			}
			Thread.sleep(4000);
		} while (bluetoothServices.isEmpty());
		return tempService;
	}

	private BluetoothGattCharacteristic getCharacteristic(BluetoothGattService service, String UUID) {
		List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
		if (characteristics == null)
			return null;

		for (BluetoothGattCharacteristic characteristic : characteristics) {
			if (characteristic.getUUID().equals(UUID))
				return characteristic;
		}
		return null;
	}
}

class BLEConnectedNotification implements BluetoothNotification<Boolean> {
	private static Logger logger = LogManager.getLogger(Connector.class);

	@Override
	public void run(Boolean arg0) {
		logger.info(arg0 ? "Device says 'Connected'" : "Device says 'Disconnected'");
	}
}

class MeasurePublisher implements Runnable {
	private Connector connector;

	public MeasurePublisher(Connector connector) {
		this.connector = connector;
	}

	@Override
	public void run() {
		connector.send(new MeasureCommand());
		connector.send(new DataDayCommand());
	}
}