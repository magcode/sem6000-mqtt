package com.github.sem2mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.sem2mqtt.configuration.BridgeConfiguration;
import com.github.sem2mqtt.configuration.BridgeConfigurationLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SemToMqttApp {
  public static final Logger LOGGER = LogManager.getLogger(SemToMqttApp.class);

  public static void main(String[] args) {
    LOGGER.info("Starting SEM6000 to MQTT bridge.");
    BridgeConfiguration bridgeConfiguration = loadBridgeConfiguration(args);
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(bridgeConfiguration);
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
