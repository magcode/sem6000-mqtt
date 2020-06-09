package org.magcode.sem6000.connector;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
	private BluetoothDevice sem6000;
	private String mac;
	private String id;
	private String pin;
	private boolean enableRegularUpdates;
	private NotificationReceiver receiver;

	public Connector(String mac, String pin, String id, boolean enableRegularUpdates, NotificationReceiver receiver) {
		this.mac = mac;
		this.id = id;
		this.pin = pin;
		this.enableRegularUpdates = enableRegularUpdates;
		this.receiver = receiver;
		this.init();
	}

	private void init() {
		try {
			// BluetoothManager manager = BluetoothManager.getBluetoothManager();
			// boolean discoveryStarted = manager.startDiscovery();

			sem6000 = getDevice(mac);
			if (sem6000 != null) {
				sem6000.enableConnectedNotifications(new BLEConnectedNotification(this));
				if (this.connect()) {
					// wait for characteristics scan
					LocalDateTime startTime = LocalDateTime.now();
					while (LocalDateTime.now().isBefore(startTime.plusSeconds(30))) {
						if (sem6000.getServicesResolved()) {
							logger.debug("[{}] Got characteristics after {} ms", this.getId(),
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
					execService = Executors.newFixedThreadPool(1, new SendReceiveThreadFactory(this.getId()));

					SendReceiveThread worker = new SendReceiveThread(workQueue, writeChar, receiver, this.getId());
					notifyChar.enableValueNotifications(worker);
					execService.submit(worker);

					Thread.sleep(500);
					workQueue.put(new LoginCommand(pin));
					Thread.sleep(1000);
					workQueue.put(new SyncTimeCommand());
					if (enableRegularUpdates) {
						this.enableRegularUpdates();
					}
				}
			}
		} catch (InterruptedException e) {
			logger.error("[{}] Could not connect to device.", this.getId(), e);
		}
	}

	public void enableRegularUpdates() {
		scheduledExecService = Executors.newScheduledThreadPool(1, new RequestMeasurementThreadFactory(this.getId()));
		Runnable measurePublisher = new MeasurePublisher(this);
		scheduledExecService.scheduleAtFixedRate(measurePublisher, 2, 10, TimeUnit.SECONDS);
	}

	public String getId() {
		return this.id;
	}

	private boolean connect() {
		try {
			if (sem6000 != null && sem6000.getConnected()) {
				logger.debug("[{}] Already connected", this.getId());
				return true;
			}

			if (sem6000 != null && !sem6000.getConnected()) {
				logger.debug("[{}] Trying to connect", this.getId());
				return sem6000.connect();
			}
		} catch (BluetoothException e) {
			logger.error("[{}] Could not connect", this.getId(), e);
		}
		return false;
	}

	private void disconnect() {
		try {
			if (sem6000 != null && sem6000.getConnected()) {
				logger.debug("[{}] Trying to disconnect", this.getId());
				sem6000.disconnect();
			}
		} catch (BluetoothException e) {
			logger.error("[{}] Could not disconnect", this.getId(), e);
		}
	}

	private synchronized void ensureConnection() {
		if (sem6000 != null && !sem6000.getConnected()) {
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
		try {
			if (this.scheduledExecService != null) {
				this.scheduledExecService.shutdownNow();
				if (!scheduledExecService.awaitTermination(100, TimeUnit.MICROSECONDS)) {
					logger.trace("Still waiting for termination ...");
				}
			}
		} catch (InterruptedException e) {
			logger.warn("Could not terminate scheduledExecService.");
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
			if (notifyChar != null) {
				notifyChar.disableValueNotifications();
				Thread.sleep(500);
			}
			this.disconnect();
			Thread.sleep(500);
			if (this.sem6000 != null) {
				this.sem6000.disableConnectedNotifications();
				Thread.sleep(2000);
			}
		} catch (InterruptedException e) {
			logger.warn("Error during stopping.", e);
		}
		logger.debug("[{}] Stopped.", this.getId());

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
				logger.trace("[{}] Services exposed by device: {}", this.getId(), service.getUUID());
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
	private String id;

	public BLEConnectedNotification(Connector connector) {
		this.id = connector.getId();
	}

	@Override
	public void run(Boolean connected) {
		if (connected) {
			logger.info("[{}] Device reports 'Connected'", this.id);
		} else {
			logger.info("[{}] Device reports 'Disconnected'", this.id);
		}
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

class SendReceiveThreadFactory implements ThreadFactory {
	private String id;

	public SendReceiveThreadFactory(String id) {
		this.id = id;
	}

	public Thread newThread(Runnable r) {
		return new Thread(r, "SendReceive-" + this.id);
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