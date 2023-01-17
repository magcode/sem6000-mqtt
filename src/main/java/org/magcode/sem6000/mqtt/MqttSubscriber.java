package org.magcode.sem6000.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.magcode.sem6000.connector.ConnectionManager;
import org.magcode.sem6000.connector.send.LedCommand;
import org.magcode.sem6000.connector.send.MeasureCommand;
import org.magcode.sem6000.connector.send.SwitchCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttSubscriber implements MqttCallback {

  private static final Logger logger = LoggerFactory.getLogger(MqttSubscriber.class);
  private ConnectionManager manager;
	private String rootTopic;

	public MqttSubscriber(String rootTopic) {
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
		String full = topic.substring(topic.indexOf(rootTopic + "/") + 1);
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
}
