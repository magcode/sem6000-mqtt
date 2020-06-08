package org.magcode.sem6000;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.magcode.sem6000.connector.SendReceiveThread;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Sem6000MQTT {
	static boolean running = true;
	public static final String UUID_NOTIFY = "0000fff4-0000-1000-8000-00805f9b34fb";
	public static final String UUID_WRITE = "0000fff3-0000-1000-8000-00805f9b34fb";
	public static final String UUID_READ = "00001800-0000-1000-8000-00805f9b34fb";
	private static BlockingQueue<Command> workQueue = null;
	private static ExecutorService service = null;
	private static Logger logger = LogManager.getLogger(Sem6000MQTT.class);

	static void printDevice(BluetoothDevice device) {
		System.out.print("Address = " + device.getAddress());
		System.out.print(" Name = " + device.getName());
		System.out.print(" Connected = " + device.getConnected());
		System.out.println();
	}

	/*
	 * After discovery is started, new devices will be detected. We can get a list
	 * of all devices through the manager's getDevices method. We can the look
	 * through the list of devices to find the device with the MAC which we provided
	 * as a parameter. We continue looking until we find it, or we try 15 times (1
	 * minutes).
	 */
	static BluetoothDevice getDevice(String address) throws InterruptedException {
		BluetoothManager manager = BluetoothManager.getBluetoothManager();
		BluetoothDevice sensor = null;
		for (int i = 0; (i < 15) && running; ++i) {
			List<BluetoothDevice> list = manager.getDevices();
			if (list == null)
				return null;

			for (BluetoothDevice device : list) {
				printDevice(device);
				/*
				 * Here we check if the address matches.
				 */
				if (device.getAddress().equals(address)) {

					sensor = device;
				}
			}

			if (sensor != null) {
				return sensor;
			}
			Thread.sleep(4000);
		}
		return null;
	}

	static BluetoothGattService getService(BluetoothDevice device, String UUID) throws InterruptedException {
		System.out.println("Services exposed by device:");
		BluetoothGattService tempService = null;
		List<BluetoothGattService> bluetoothServices = null;
		do {
			bluetoothServices = device.getServices();
			if (bluetoothServices == null)
				return null;

			for (BluetoothGattService service : bluetoothServices) {
				System.out.println("UUID: " + service.getUUID());
				if (service.getUUID().equals(UUID))
					tempService = service;
			}
			Thread.sleep(4000);
		} while (bluetoothServices.isEmpty() && running);
		return tempService;
	}

	static BluetoothGattCharacteristic getCharacteristic(BluetoothGattService service, String UUID) {
		List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
		if (characteristics == null)
			return null;

		for (BluetoothGattCharacteristic characteristic : characteristics) {
			if (characteristic.getUUID().equals(UUID))
				return characteristic;
		}
		return null;
	}

	public static void main(String[] args) throws InterruptedException {

		if (args.length < 1) {
			System.err.println("Run with <device_address> argument");
			System.exit(-1);
		}
		logger.info("Start with {}", args[0]);
		BluetoothManager manager = BluetoothManager.getBluetoothManager();
		boolean discoveryStarted = manager.startDiscovery();
		BluetoothDevice sensor = getDevice(args[0]);
		sensor.enableConnectedNotifications(new ConnectedNotification());

		try {
			sensor.connect();
		} catch (BluetoothException bleE) {
			System.out.println("Connection failure");
			System.exit(-1);
		}

		// BluetoothGattService sensorService = sensor.find("0xfff0");
		BluetoothGattService sensorService = getService(sensor, "0000fff0-0000-1000-8000-00805f9b34fb");
		if (sensorService == null) {
			System.err.println("This device does not have the service.");
			sensor.disconnect();
			System.exit(-1);
		}

		BluetoothGattCharacteristic writeChar = getCharacteristic(sensorService, UUID_WRITE);
		BluetoothGattCharacteristic notifyChar = getCharacteristic(sensorService, UUID_NOTIFY);
		workQueue = new LinkedBlockingQueue<Command>(10);
		service = Executors.newFixedThreadPool(1);
		SendReceiveThread worker = new SendReceiveThread(workQueue, writeChar, null);
		notifyChar.enableValueNotifications(worker);
		service.submit(worker);

		workQueue.put(new LoginCommand("0000"));
		workQueue.put(new SyncTimeCommand());
		Thread.sleep(500);

		workQueue.put(new DataDayCommand());
		Thread.sleep(5000);

		workQueue.put(new MeasureCommand());
		Thread.sleep(5000);

		workQueue.put(new MeasureCommand());
		Thread.sleep(5000);

		workQueue.put(new MeasureCommand());
		Thread.sleep(5000);

		workQueue.put(new MeasureCommand());
		Thread.sleep(5000);

		workQueue.put(new MeasureCommand());
		Thread.sleep(5000);

		Thread.sleep(10000);
		sensor.disconnect();
		System.exit(-1);
	}

	public static String byteArrayToHex(byte[] a) {
		StringBuilder sb = new StringBuilder(a.length * 2);
		for (byte b : a)
			sb.append(String.format("%02x", b));
		return sb.toString();
	}
}
