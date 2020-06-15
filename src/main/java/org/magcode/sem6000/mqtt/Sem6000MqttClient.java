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
import java.util.Properties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.config.Configurator;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.magcode.sem6000.connector.ConnectionManager;

public class Sem6000MqttClient {
	private static Logger logger = LogManager.getLogger(Sem6000MqttClient.class);

	private static MqttClient mqttClient;
	private static String rootTopic = "home/sem";
	private static String mqttServer = "tcp://broker";
	private static final int MAX_INFLIGHT = 200;
	private static Map<String, Sem6000Config> sems;
	private static ConnectionManager conMan;
	private static MqttSubscriber mqttSubscriber;
	private static String logLevel = "INFO";

	public static void main(String[] args) throws Exception {
		logger.info("Started");
		Thread.sleep(5000);
		sems = new HashMap<String, Sem6000Config>();
		readProps();
		reConfigureLogger();
		startMQTTClient();
		conMan = new ConnectionManager(new MqttPublisher(mqttClient, rootTopic));
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
				Logger logger2 = LogManager.getLogger("shutdown");
				try {
					conMan.shutDown();
					for (Entry<String, Sem6000Config> entry : sems.entrySet()) {
						Sem6000Config value = entry.getValue();
						MqttMessage message = new MqttMessage();
						message.setPayload("lost".getBytes());
						message.setRetained(true);
						String deviceTopic = rootTopic + "/" + value.getName();
						mqttClient.publish(deviceTopic + "/$state", message);
						logger2.info("Published '{}' to '{}'", message, deviceTopic + "/$state");
					}

					mqttClient.disconnect();
					logger2.info("Disconnected from MQTT server");
					logger2.info("Stopped.");
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
		mqttSubscriber = new MqttSubscriber(rootTopic);
		mqttClient.setCallback(mqttSubscriber);
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
			String subTopic = rootTopic + "/" + value.getName() + "/+/set";
			mqttClient.subscribe(subTopic);
			logger.info("Subscribed to {}", subTopic);
		}
	}

	private static void reConfigureLogger() {
		Configurator.setRootLevel(Level.forName(logLevel, 0));
	}

	private static void readProps() {
		Properties props = new Properties();
		InputStream input = null;

		try {
			File jarPath = new File(
					Sem6000MqttClient.class.getProtectionDomain().getCodeSource().getLocation().getPath());
			String propertiesPath = jarPath.getParentFile().getAbsolutePath();
			String filePath = propertiesPath + "/sem6000.properties";
			logger.info("Loading properties from " + filePath);
			input = new FileInputStream(filePath);
			props.load(input);

			rootTopic = props.getProperty("rootTopic", "home");
			mqttServer = props.getProperty("mqttServer", "tcp://localhost");
			logLevel = props.getProperty("logLevel", "INFO");
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