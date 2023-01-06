package com.github.sem2mqtt.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import java.util.Set;

public class BridgeConfiguration {

  private final MqttConfig mqttConfig;
  private final Set<Sem6000Config> semConfigs;

  public BridgeConfiguration(@JsonProperty(value = "mqtt", defaultValue = "") MqttConfig mqttConfig,
      @JsonProperty(value = "sem", defaultValue = "") Set<Sem6000Config> semConfigs) {
    this.mqttConfig = Optional.ofNullable(mqttConfig).orElse(MqttConfig.defaults());
    this.semConfigs = semConfigs;
  }

  public MqttConfig getMqttConfig() {
    return mqttConfig;
  }

  public Set<Sem6000Config> getSemConfigs() {
    return semConfigs;
  }
}
