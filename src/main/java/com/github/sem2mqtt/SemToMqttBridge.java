package com.github.sem2mqtt;

import com.github.sem2mqtt.configuration.Sem6000Config;
import com.github.sem2mqtt.mqtt.MqttConnection;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class SemToMqttBridge {

  private static final Logger LOGGER = LogManager.getLogger(SemToMqttBridge.class);

  private final MqttConnection mqttConnection;
  private final Set<Sem6000Config> sem6000Configs;
  private final String rootTopic;

  public SemToMqttBridge(MqttConnection mqttConnection, String rootTopic, Set<Sem6000Config> sem6000Configs) {

    this.mqttConnection = mqttConnection;
    this.sem6000Configs = sem6000Configs;
    this.rootTopic = rootTopic;
  }

  public void run() {
    LOGGER.info("Starting bridge service.");
    mqttConnection.establish();
    sem6000Configs.forEach(this::subscribeToSem6000);
  }

  void subscribeToSem6000(Sem6000Config sem6000Config) {
    mqttConnection.subscribe(String.format("%s/%s/+/set", rootTopic, sem6000Config.getName()),
        (String topic, MqttMessage message) -> handleMqttMessage(topic, message, sem6000Config));
  }

  void handleMqttMessage(String topic, MqttMessage message, Sem6000Config sem6000Config) {

  }
}
