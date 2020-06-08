package org.magcode.sem6000;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.magcode.sem6000.receive.SemResponse;
import org.magcode.sem6000.send.MeasureCommand;

public class MqttClient {
	private static Logger logger = LogManager.getLogger(MqttClient.class);

	public static void main(String[] args) throws InterruptedException {

		Connector sem1 = new Connector("18:62:E4:11:9A:C1", "0000", new Receiver());
		sem1.connect();
		Thread.sleep(3000);
		sem1.send(new MeasureCommand());
		Thread.sleep(3000);
		sem1.disconnect();

	}

}

class Receiver implements NotificatioReceiver {
	private static Logger logger = LogManager.getLogger(Receiver.class);

	@Override
	public void receiveSem6000Response(SemResponse response) {
		logger.info(response.toString());

	}

}