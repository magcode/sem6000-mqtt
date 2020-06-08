package org.magcode.sem6000.connector;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.magcode.sem6000.NotificatioReceiver;
import org.magcode.sem6000.connector.send.Command;
import org.magcode.sem6000.connector.send.LoginCommand;
import org.magcode.sem6000.connector.send.SyncTimeCommand;

import tinyb.BluetoothDevice;
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
	private ExecutorService service = null;
	private BluetoothGattCharacteristic notifyChar;
	BluetoothDevice sem6000;

	public Connector(String mac, String pin, NotificatioReceiver receiver) {
		try {
			// BluetoothManager manager = BluetoothManager.getBluetoothManager();
			// boolean discoveryStarted = manager.startDiscovery();
			sem6000 = getDevice(mac);
			if (sem6000 != null) {
				sem6000.enableConnectedNotifications(new BLEConnectedNotification());
				this.connect();
				// wait for characteristics scan
				LocalDateTime startTime = LocalDateTime.now();
				while (LocalDateTime.now().isBefore(startTime.plusSeconds(30))) {
					if (sem6000.getServicesResolved()) {
						logger.debug("Got characteristics after "
								+ ChronoUnit.MILLIS.between(startTime, LocalDateTime.now()) + "ms");
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
				service = Executors.newFixedThreadPool(1);

				SendReceiveThread worker = new SendReceiveThread(workQueue, writeChar, receiver);
				notifyChar.enableValueNotifications(worker);

				service.submit(worker);

				Thread.sleep(500);
				workQueue.put(new LoginCommand(pin));
				Thread.sleep(500);
				workQueue.put(new SyncTimeCommand());
			}
		} catch (InterruptedException e) {
			logger.error("Could not connect to device.", e);
		}
	}

	private void connect() {
		logger.trace("Trying to connect");
		sem6000.connect();
	}

	private void disconnect() {
		logger.trace("Trying to disconnect");
		sem6000.disconnect();
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
			notifyChar.disableValueNotifications();
			Thread.sleep(500);
			this.disconnect();
			Thread.sleep(500);
			this.sem6000.disableConnectedNotifications();
			Thread.sleep(500);
			this.sem6000.remove();
			Thread.sleep(500);
			this.service.shutdownNow();
			if (!service.awaitTermination(100, TimeUnit.MICROSECONDS)) {
				logger.info("Still waiting for termination ...");
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
				logger.trace("Services exposed by device: {}", service.getUUID());
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
