package org.magcode.sem6000.mqtt;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.apache.logging.log4j.Logger;
import org.magcode.sem6000.connector.ConnectionManager;
import org.magcode.sem6000.connector.send.LedCommand;
import org.magcode.sem6000.connector.send.SwitchCommand;

public class Subscriber implements MqttCallback {
	private static Logger logger = LogManager.getLogger(Subscriber.class);
	private ConnectionManager manager;
	private String rootTopic;

	public Subscriber(String rootTopic) {
		this.rootTopic = rootTopic;
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
			SwitchCommand switcher = new SwitchCommand(onOff);
			if (manager != null) {
				manager.sendCommand(node, switcher);
			}
			break;
		case "led":
			Boolean ledOnOff = Boolean.valueOf(message.toString());
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
}
