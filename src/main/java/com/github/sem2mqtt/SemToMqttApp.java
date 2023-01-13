package com.github.sem2mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.sem2mqtt.configuration.BridgeConfiguration;
import com.github.sem2mqtt.configuration.BridgeConfigurationLoader;
import com.github.sem2mqtt.configuration.MqttConfig;
import com.github.sem2mqtt.mqtt.MqttConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class SemToMqttApp {

  public static final Logger LOGGER = LogManager.getLogger(SemToMqttApp.class);

  public static void main(String[] args) {
    LOGGER.info("Starting SEM6000 to MQTT bridge.");
    BridgeConfiguration bridgeConfiguration = loadBridgeConfiguration(args);
    MqttConnection mqttConnection;
    MqttConfig mqttConfig = bridgeConfiguration.getMqttConfig();
    try {
      mqttConnection = new MqttConnection(
          new MqttClient(mqttConfig.getUrl(), mqttConfig.getClientId(), new MemoryPersistence()), mqttConfig);
    } catch (MqttException e) {
      throw new RuntimeException("Failed to set up mqtt client: ", e);
    }
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(mqttConnection,
        mqttConfig.getRootTopic(), bridgeConfiguration.getSemConfigs());

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
