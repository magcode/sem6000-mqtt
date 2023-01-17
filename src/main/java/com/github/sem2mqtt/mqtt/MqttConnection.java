package com.github.sem2mqtt.mqtt;

import com.github.sem2mqtt.configuration.MqttConfig;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttConnection implements MqttCallback {

  private static final Logger LOGGER = LoggerFactory.getLogger(MqttConnection.class);

  private final MqttClient mqttClient;
  private final MqttConfig mqttConfig;
  // a higher number of messages "in flight" is expected, as it increases with every sem6000.
  // their data is processed in parallel
  private static final int MAX_INFLIGHT = 200;

  public MqttConnection(MqttClient mqttClient, MqttConfig mqttConfig) {
    this.mqttClient = mqttClient;
    this.mqttConfig = mqttConfig;
  }

  public void establish() {
    mqttClient.setCallback(this);
    try {
      MqttConnectOptions mqttConnectOptions = getMqttConnectOptions();
      mqttClient.connect(mqttConnectOptions);
      LOGGER.info("Established connection to mqtt server");
    } catch (MqttSecurityException e) {
      throw new RuntimeException("Not authorized to access mqtt server: ", e);
    } catch (MqttException e) {
      throw new RuntimeException("Failed to connect to mqtt server: ", e);
    }
  }

  private MqttConnectOptions getMqttConnectOptions() {
    MqttConnectOptions connOpt = new MqttConnectOptions();
    connOpt.setCleanSession(true);
    connOpt.setMaxInflight(MAX_INFLIGHT);
    connOpt.setAutomaticReconnect(true);
    if (mqttConfig.hasCredentials()) {
      connOpt.setUserName(mqttConfig.getUsername());
      connOpt.setPassword(mqttConfig.getPassword().toCharArray());
    }
    return connOpt;
  }

  @Override
  public void connectionLost(Throwable cause) {
    LOGGER.warn("Connection to mqtt server is lost. Reconnecting...");
  }

  @Override
  public void messageArrived(String topic, MqttMessage message) {
    LOGGER.debug("Received message on topic '{}'", topic);
  }

  @Override
  public void deliveryComplete(IMqttDeliveryToken token) {
    LOGGER.debug("Message delivery to topic '{}' completed", String.join(", ", token.getTopics()));
  }

  public void subscribe(String topic, MessageCallback callback) {
    try {
      mqttClient.subscribe(topic, callback::handleMqttMessage);
      LOGGER.info("Subscribed to topic '{}'", topic);
    } catch (MqttException e) {
      throw new RuntimeException(String.format("Failed to subscribe to mqtt topic %s: ", topic), e);
    }
  }

  public interface MessageCallback {

    void handleMqttMessage(String topic, MqttMessage message);
  }
}
