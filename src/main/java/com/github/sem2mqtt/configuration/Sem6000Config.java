package com.github.sem2mqtt.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.util.Optional;

public class Sem6000Config {

  public static final String DEFAULT_PIN = "0000"; // default pin after factory reset
  private static final Duration DEFAULT_UPDATE_INTERVAL = Duration.ofMinutes(1);
  private final String mac;
  private final String pin;
  private final String name;
  private final Duration updateInterval;

  @JsonCreator
  public Sem6000Config(@JsonProperty("mac") String mac, @JsonProperty(value = "pin") String pin,
      @JsonProperty("name") String name,
      @JsonProperty(value = "updateInterval") Duration updateInterval) {
    this.mac = mac;
    this.pin = Optional.ofNullable(pin).orElse(DEFAULT_PIN);
    this.name = name;
    this.updateInterval = Optional.ofNullable(updateInterval).orElse(DEFAULT_UPDATE_INTERVAL);
  }


  public String getMac() {
    return mac;
  }

  public String getPin() {
    return pin;
  }

  public String getName() {
    return name;
  }

  public Duration getUpdateInterval() {
    return updateInterval;
  }
}
