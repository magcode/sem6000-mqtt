package org.magcode.sem6000;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LifeCycle;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.magcode.sem6000.connector.Connector;
import org.magcode.sem6000.connector.NotificationReceiver;
import org.magcode.sem6000.connector.receive.DataDayResponse;
import org.magcode.sem6000.connector.receive.MeasurementResponse;
import org.magcode.sem6000.connector.receive.SemResponse;
import org.magcode.sem6000.connectorv2.ConnectorV2;

import tinyb.BluetoothManager;

public class Sem6000MqttClient {
	private static Logger logger = LogManager.getLogger(Sem6000MqttClient.class);
	private static boolean retained = false;
	private static int qos = 0;
	private static MqttClient mqttClient;
	private static String rootTopic = "home/sem";
	private static String mqttServer = "tcp://broker";
	private static final int MAX_INFLIGHT = 200;
	private static Map<String, Sem6000Config> sems;

	public static void main(String[] args) throws Exception {
		logger.info("Started");

		// BluetoothManager manager = BluetoothManager.getBluetoothManager();
		// manager.startDiscovery();
		Thread.sleep(5000);
		sems = new HashMap<String, Sem6000Config>();
		Sem6000Config s1 = new Sem6000Config("18:62:E4:11:9A:C1", "0000", "sem61", 50);
		sems.put("sem61", s1);
		Sem6000Config s2 = new Sem6000Config("2C:AB:33:01:17:04", "0000", "sem62", 50);
		sems.put("sem62", s2);

		startMQTTClient();
		for (Entry<String, Sem6000Config> entry : sems.entrySet()) {
			Sem6000Config value = entry.getValue();
			ConnectorV2 sem = new ConnectorV2(value.getMac(), value.getPin(), value.getName(), true,
					new Receiver(mqttClient, rootTopic));
			Thread.sleep(10000);
			value.setConnector(sem);
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				Logger logger2 = LogManager.getLogger("shutdown");
				try {

					for (Entry<String, Sem6000Config> entry : sems.entrySet()) {
						Sem6000Config value = entry.getValue();
						ConnectorV2 sem = value.getConnector();
						sem.stop();
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						MqttMessage message = new MqttMessage();
						message.setPayload("lost".getBytes());
						message.setRetained(true);
						String deviceTopic = rootTopic + "/" + value.getName();
						mqttClient.publish(deviceTopic + "/$state", message);
						logger2.info("Published '{}' to '{}'", message, deviceTopic + "/$state");
					}

					mqttClient.disconnect();
					logger2.info("Disconnected from MQTT server");
					((LifeCycle) LogManager.getContext()).stop();
				} catch (MqttException e) {
					logger2.error("Error during shutdown", e);
				}
			}
		});
	}

	private static void startMQTTClient() throws MqttException {
		String hostName = "";
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			logger.error("Failed to get hostname", e);
		}
		mqttClient = new MqttClient(mqttServer, "client-for-sem6000-on-" + hostName, new MemoryPersistence());
		MqttConnectOptions connOpt = new MqttConnectOptions();
		connOpt.setCleanSession(true);
		connOpt.setMaxInflight(MAX_INFLIGHT);
		connOpt.setAutomaticReconnect(true);
		mqttClient.setCallback(new Subscriber(rootTopic));
		mqttClient.connect();
		logger.info("Connected to MQTT broker.");
		try {
			// give some time before subscribing
			Thread.sleep(200);
		} catch (InterruptedException e) {
			//
		}
		for (Entry<String, Sem6000Config> entry : sems.entrySet()) {
			Sem6000Config value = entry.getValue();
			String subTopic = rootTopic + "/" + value.getName() + "/power/+/set";
			mqttClient.subscribe(subTopic);
			logger.info("Subscribed to {}", subTopic);
		}
	}

}

class Receiver implements NotificationReceiver {
	private static Logger logger = LogManager.getLogger(Receiver.class);
	private MqttClient mqttClient;
	private String topic;

	public Receiver(MqttClient mqttClient, String topic) {
		this.mqttClient = mqttClient;
		this.topic = topic;
	}

	@Override
	public void receiveSem6000Response(SemResponse response) {
		if (response == null || response.getType() == null) {
			logger.error("here");
		}
		switch (response.getType()) {
		case measure: {
			MeasurementResponse mr = (MeasurementResponse) response;
			publish(topic + "/" + mr.getId() + "/voltage", mr.getVoltage());
			publish(topic + "/" + mr.getId() + "/power", mr.getPower());
			break;
		}
		case dataday: {
			DataDayResponse mr = (DataDayResponse) response;
			publish(topic + "/" + mr.getId() + "/energytoday", mr.getToday());
			break;
		}
		default:
			break;
		}
		logger.info(response.toString());
	}

	protected void publish(String topic, String payload) {
		MqttMessage message = new MqttMessage(payload.getBytes());
		publish(topic, message);
	}

	protected void publish(String topic, boolean payload) {
		publish(topic, Boolean.valueOf(payload).toString());
	}

	protected void publish(String topic, float payload) {
		publish(topic, Float.toString(payload));
	}

	protected void publish(String topic, int payload) {
		publish(topic, Integer.toString(payload));
	}

	private void publish(String topic, MqttMessage message) {
		try {
			message.setRetained(false);
			message.setQos(0);
			this.mqttClient.publish(topic, message);
		} catch (MqttPersistenceException e) {
			logger.error("MqttPersistenceException", e);
		} catch (MqttException e) {
			logger.error("MqttException", e);
		}
	}
}