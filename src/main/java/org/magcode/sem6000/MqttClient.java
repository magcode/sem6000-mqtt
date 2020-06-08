package org.magcode.sem6000;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.magcode.sem6000.connector.Connector;
import org.magcode.sem6000.connector.receive.SemResponse;

import tinyb.BluetoothManager;

public class MqttClient {
	private static Logger logger = LogManager.getLogger(MqttClient.class);

	public static void main(String[] args) throws InterruptedException {
		logger.info("Started");

		BluetoothManager manager = BluetoothManager.getBluetoothManager();
		manager.startDiscovery();
		Thread.sleep(5000);

		Connector sem1 = new Connector("2C:AB:33:01:17:04", "0000", "sem61", true, new Receiver());
		Connector sem2 = new Connector("18:62:E4:11:9A:C1", "0000", "sem62", true, new Receiver());

		Thread.sleep(60000);
		sem1.stop();
		sem2.stop();
		Thread.sleep(10000);
		System.exit(-1);
	}
}

class Receiver implements NotificatioReceiver {
	private static Logger logger = LogManager.getLogger(Receiver.class);

	@Override
	public void receiveSem6000Response(SemResponse response) {
		logger.info(response.toString());
	}
}