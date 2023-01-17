package com.github.sem2mqtt;

import com.coreoz.wisp.Scheduler;
import com.github.sem2mqtt.bluetooth.BluetoothConnectionManager;
import com.github.sem2mqtt.bluetooth.sem6000.Sem6000Connection;
import com.github.sem2mqtt.configuration.Sem6000Config;
import com.github.sem2mqtt.mqtt.MqttConnection;
import com.github.sem2mqtt.mqtt.MqttConnection.MessageCallback;
import java.util.Set;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemToMqttBridge {

  private static final Logger LOGGER = LoggerFactory.getLogger(SemToMqttBridge.class);

  private final MqttConnection mqttConnection;
  private final Set<Sem6000Config> sem6000Configs;
  private final String rootTopic;
  private final BluetoothConnectionManager bluetoothConnectionManager;
  private final Scheduler scheduler;

  public SemToMqttBridge(String rootTopic, Set<Sem6000Config> sem6000Configs, MqttConnection mqttConnection,
      BluetoothConnectionManager bluetoothConnectionManager, Scheduler scheduler) {

    this.mqttConnection = mqttConnection;
    this.sem6000Configs = sem6000Configs;
    this.rootTopic = rootTopic;
    this.bluetoothConnectionManager = bluetoothConnectionManager;
    this.scheduler = scheduler;
  }

  public void run() {
    LOGGER.info("Starting bridge service.");
    mqttConnection.establish();
    bluetoothConnectionManager.init();
    sem6000Configs.stream().peek(this::subscribeToSem6000MqttTopics)
        .map(sem6000Config -> bluetoothConnectionManager.setupConnection(
            new Sem6000Connection(sem6000Config, bluetoothConnectionManager, scheduler)))
        .forEach(Sem6000Connection::establish);
  }

  void subscribeToSem6000MqttTopics(Sem6000Config sem6000Config) {
    mqttConnection.subscribe(String.format("%s/%s/+/set", rootTopic, sem6000Config.getName()),
        createMessageCallbackFor(sem6000Config));
  }

  private MessageCallback createMessageCallbackFor(Sem6000Config sem6000Config) {
    return (String topic, MqttMessage message) -> handleMqttMessage(topic, message, sem6000Config);
  }

  void handleMqttMessage(String topic, MqttMessage message, Sem6000Config sem6000Config) {

  }
}
