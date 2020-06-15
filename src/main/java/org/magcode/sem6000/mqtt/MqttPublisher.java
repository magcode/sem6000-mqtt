package org.magcode.sem6000.mqtt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.magcode.sem6000.connector.NotificationConsumer;
import org.magcode.sem6000.connector.receive.DataDayResponse;
import org.magcode.sem6000.connector.receive.MeasurementResponse;
import org.magcode.sem6000.connector.receive.SemResponse;

class MqttPublisher implements NotificationConsumer {
	private static Logger logger = LogManager.getLogger(MqttPublisher.class);
	private MqttClient mqttClient;
	private String topic;

	public MqttPublisher(MqttClient mqttClient, String topic) {
		this.mqttClient = mqttClient;
		this.topic = topic;
	}

	@Override
	public void receiveSem6000Response(SemResponse response) {
		if (response == null || response.getType() == null) {
			logger.error("Invalid response");
			return;
		}
		switch (response.getType()) {
		case measure: {
			MeasurementResponse mr = (MeasurementResponse) response;
			publish(topic + "/" + mr.getId() + "/voltage", mr.getVoltage());
			publish(topic + "/" + mr.getId() + "/power", mr.getPower());
			publish(topic + "/" + mr.getId() + "/relay", mr.isPowerOn());
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
		logger.debug(response.toString());
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