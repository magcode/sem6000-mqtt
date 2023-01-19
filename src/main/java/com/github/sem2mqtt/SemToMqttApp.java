package com.github.sem2mqtt;

import com.coreoz.wisp.Scheduler;
import com.coreoz.wisp.SchedulerConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.sem2mqtt.bluetooth.BluetoothConnectionManager;
import com.github.sem2mqtt.configuration.BridgeConfiguration;
import com.github.sem2mqtt.configuration.BridgeConfigurationLoader;
import com.github.sem2mqtt.configuration.MqttConfig;
import com.github.sem2mqtt.mqtt.MqttConnection;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemToMqttApp {

  private static final Logger LOGGER = LoggerFactory.getLogger(SemToMqttApp.class);

  public static void main(String[] args) {
    LOGGER.info("Starting SEM6000 to MQTT bridge.");
    BridgeConfiguration bridgeConfiguration = loadBridgeConfiguration(args);
    MqttConnection mqttConnection;
    MqttConfig mqttConfig = bridgeConfiguration.getMqttConfig();
    MqttClient mqttClient;
    try {
      mqttClient = new MqttClient(mqttConfig.getUrl(), mqttConfig.getClientId(), new MemoryPersistence());
      mqttConnection = new MqttConnection(mqttClient, mqttConfig);
    } catch (MqttException e) {
      throw new RuntimeException("Failed to set up mqtt client: ", e);
    }
    Scheduler scheduler = new Scheduler(SchedulerConfig.builder().maxThreads(4).build());
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(mqttConfig.getRootTopic(),
        bridgeConfiguration.getSemConfigs(), mqttConnection, new BluetoothConnectionManager(), scheduler);

    semToMqttBridge.run();
  }

  private static BridgeConfiguration loadBridgeConfiguration(String[] args) {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.findAndRegisterModules();
    BridgeConfigurationLoader bridgeConfigurationLoader = new BridgeConfigurationLoader(mapper);
    BridgeConfiguration bridgeConfiguration;
    if (args.length == 1) {
      bridgeConfiguration = bridgeConfigurationLoader.load(args[0]);
    } else {
      bridgeConfiguration = bridgeConfigurationLoader.load();
    }
    return bridgeConfiguration;
  }
}
