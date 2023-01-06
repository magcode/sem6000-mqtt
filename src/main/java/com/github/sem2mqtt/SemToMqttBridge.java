package com.github.sem2mqtt;

import com.github.sem2mqtt.configuration.BridgeConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SemToMqttBridge {
  public static final Logger LOGGER = LogManager.getLogger(SemToMqttBridge.class);

  private final BridgeConfiguration bridgeConfiguration;

  public SemToMqttBridge(BridgeConfiguration bridgeConfiguration) {

    this.bridgeConfiguration = bridgeConfiguration;
  }

  public void run() {
    LOGGER.info("Starting bridge service.");
  }
}
