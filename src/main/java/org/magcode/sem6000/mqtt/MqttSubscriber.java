package org.magcode.sem6000.mqtt;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.apache.logging.log4j.Logger;
import org.magcode.sem6000.connector.ConnectionManager;
import org.magcode.sem6000.connector.send.LedCommand;
import org.magcode.sem6000.connector.send.MeasureCommand;
import org.magcode.sem6000.connector.send.SwitchCommand;

public class MqttSubscriber implements MqttCallbackExtended {
	private static Logger logger = LogManager.getLogger(MqttSubscriber.class);
	private ConnectionManager manager;
	private String rootTopic;
	private final MqttClient mqttClient;
	private Map<String, Sem6000Config> sems;

	public MqttSubscriber(MqttClient mqttClient, String rootTopic, Map<String, Sem6000Config> sems) {
		this.rootTopic = rootTopic;
		this.sems = sems;
		this.mqttClient = mqttClient;
	}

	public void setConnectionManager(ConnectionManager manager) {
		this.manager = manager;
	}

	@Override
	public void connectionLost(Throwable cause) {
		logger.error("MQTT connection lost");
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		String full = StringUtils.substringAfter(topic, rootTopic + "/");
		String[] parts = full.split("/");
		String node = parts[0];
		String prop = parts[1];
		switch (prop) {
			case "relay":
				Boolean onOff = Boolean.valueOf(message.toString());
				logger.info("[{}] Sending 'switch' {}", node, onOff);
				SwitchCommand switcher = new SwitchCommand(onOff);
				if (manager != null) {
					manager.sendCommand(node, switcher);
					MeasureCommand measure = new MeasureCommand();
					manager.sendCommand(node, measure);
				}
				break;
			case "led":
				Boolean ledOnOff = Boolean.valueOf(message.toString());
				logger.info("[{}] Sending 'led' {}", node, ledOnOff);
				LedCommand ledCommand = new LedCommand(ledOnOff);
				if (manager != null) {
					manager.sendCommand(node, ledCommand);
				}
				break;
			default:
				logger.info("Ignoring command {} sent on {}", message.toString(), topic);
				break;
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		//
	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		logger.info("Connected to '{}', reconnect: {}", serverURI, reconnect);
		try {
			for (Entry<String, Sem6000Config> entry : this.sems.entrySet()) {
				Sem6000Config value = entry.getValue();
				String subTopic = rootTopic + "/" + value.getName() + "/+/set";
				this.mqttClient.subscribe(subTopic);
				logger.info("Subscribed to {}", subTopic);
			}
		} catch (MqttException e) {
			logger.error("Caught exception while subscribing!", e);
		}
	}
}
