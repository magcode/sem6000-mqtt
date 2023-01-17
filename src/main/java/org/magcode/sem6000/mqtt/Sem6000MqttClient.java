package org.magcode.sem6000.mqtt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.magcode.sem6000.connector.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sem6000MqttClient {

	private static final Logger logger = LoggerFactory.getLogger(Sem6000MqttClient.class);

	public static MqttClient mqttClient;
	private static String rootTopic = "home/sem";
	private static String mqttServer = "tcp://broker";
	private static boolean mqttHasCredentials = false;
	private static String mqttClientId;
	private static String mqttUsername;
	private static String mqttPassword;
	private static int consecutiveReconnectLimit = 100;
	private static final int MAX_INFLIGHT = 200;
	private static Map<String, Sem6000Config> sems;
	private static ConnectionManager conMan;
	public static MqttSubscriber mqttSubscriber;

	public static void main(String[] args) throws Exception {
		logger.info("Started");
		Thread.sleep(5000);
		sems = new HashMap<String, Sem6000Config>();
		readProps();
		startMQTTClient();
		conMan = new ConnectionManager(new MqttPublisher(mqttClient, rootTopic), consecutiveReconnectLimit);
		conMan.init();
		mqttSubscriber.setConnectionManager(conMan);

		for (Entry<String, Sem6000Config> entry : sems.entrySet()) {
			Sem6000Config value = entry.getValue();
			conMan.addSem(value);
			Thread.sleep(2000);
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				Logger logger2 = LoggerFactory.getLogger("shutdown");
				try {
					conMan.shutDown();
					for (Entry<String, Sem6000Config> entry : sems.entrySet()) {
						Sem6000Config value = entry.getValue();
						MqttMessage message = new MqttMessage();
						message.setPayload("lost".getBytes());
						message.setRetained(false);
						String deviceTopic = rootTopic + "/" + value.getName();
						mqttClient.publish(deviceTopic + "/state", message);
						logger2.info("Published '{}' to '{}'", message, deviceTopic + "/$state");
					}
					mqttClient.disconnect();
					logger2.info("Disconnected from MQTT server");
					logger2.info("Stopped.");
				} catch (MqttException e) {
					logger2.error("Error during shutdown", e);
				}
			}
		});
	}

	public static void startMQTTClient() throws MqttException {
		mqttClient = new MqttClient(mqttServer, Optional.ofNullable(mqttClientId).orElse(generateClientId()), new MemoryPersistence());
		MqttConnectOptions connOpt = new MqttConnectOptions();
		connOpt.setCleanSession(true);
		connOpt.setMaxInflight(MAX_INFLIGHT);
		connOpt.setAutomaticReconnect(true);
		if (mqttHasCredentials) {
			connOpt.setUserName(mqttUsername);
			connOpt.setPassword(mqttPassword.toCharArray());
		}
		mqttSubscriber = new MqttSubscriber(rootTopic);
		mqttClient.setCallback(mqttSubscriber);
		mqttClient.connect(connOpt);
		logger.info("Connected to MQTT broker.");
		try {
			// give some time before subscribing
			Thread.sleep(200);
		} catch (InterruptedException e) {
			//
		}
		for (Entry<String, Sem6000Config> entry : sems.entrySet()) {
			Sem6000Config value = entry.getValue();
			String subTopic = rootTopic + "/" + value.getName() + "/+/set";
			mqttClient.subscribe(subTopic);
			logger.info("Subscribed to {}", subTopic);
		}
	}

	private static String generateClientId() {
		String hostName = "";
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			logger.error("Failed to get hostname", e);
		}
		return "client-for-sem6000-on-" + hostName;
	}

	private static void readProps() {
		Properties props = new Properties();
		InputStream input = null;

		try {
			File jarPath = new File(
					Sem6000MqttClient.class.getProtectionDomain().getCodeSource().getLocation().getPath());
			String propertiesPath = jarPath.getParentFile().getAbsolutePath();
			String filePath = propertiesPath + "/sem6.properties";
			logger.info("Loading properties from " + filePath);
			input = new FileInputStream(filePath);
			props.load(input);

			rootTopic = props.getProperty("rootTopic", "home");
			mqttServer = props.getProperty("mqttServer", "tcp://localhost");
			mqttClientId = props.getProperty("mqttClientId");
			if (props.containsKey("mqttUsername") || props.containsKey("mqttPassword")) {
				mqttHasCredentials = true;
				mqttUsername = props.getProperty("mqttUsername", "mqtt");
				mqttPassword = props.getProperty("mqttPassword", "mqttPassword");
			}
			consecutiveReconnectLimit = Integer.valueOf(props.getProperty("maxReconnects", "100"));
			Enumeration<?> e = props.propertyNames();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				for (int i = 1; i < 11; i++) {
					if (key.equals("sem" + i + ".mac")) {
						Sem6000Config one = new Sem6000Config();
						one.setMac(props.getProperty("sem" + i + ".mac"));
						one.setPin(props.getProperty("sem" + i + ".pin"));
						one.setName(props.getProperty("sem" + i + ".name"));
						one.setUpdateSeconds(Integer.valueOf(props.getProperty("sem" + i + ".refresh")));
						sems.put(one.getName(), one);
					}
				}
			}
		} catch (IOException ex) {
			logger.error("Could not read properties", ex);
			System.exit(1);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					logger.error("Failed to close file", e);
				}
			}
		}
	}
}