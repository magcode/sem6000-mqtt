package com.github.sem2mqtt.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.Optional;

public class MqttConfig {

  public static final String DEFAULT_ROOT_TOPIC = "home";
  public static final String DEFAULT_URL = "tcp://localhost";
  public static final String DEFAULT_CLIENT_ID = "semtomqttbridge";

  private final String rootTopic;
  private final String url;
  private final String clientId;
  private final String username;
  private final String password;

  @JsonCreator
  public MqttConfig(@JsonProperty(value = "rootTopic") String rootTopic,
      @JsonProperty(value = "url") String url,
      @JsonProperty(value = "clientId") String clientId,
      @JsonProperty(value = "username") String username, @JsonProperty(value = "password") String password) {
    this.rootTopic = Optional.ofNullable(rootTopic).orElse(DEFAULT_ROOT_TOPIC);
    this.url = Optional.ofNullable(url).orElse(DEFAULT_URL);
    this.clientId = Optional.ofNullable(clientId).orElse(DEFAULT_CLIENT_ID);
    this.username = username;
    this.password = password;
  }

  public static MqttConfig defaults() {
    return new MqttConfig(DEFAULT_ROOT_TOPIC, DEFAULT_URL, DEFAULT_CLIENT_ID, null,
        null);
  }

  public String getRootTopic() {
    return rootTopic;
  }

  public String getUrl() {
    return url;
  }

  public String getClientId() {
    return clientId;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public boolean hasCredentials() {
    return Objects.nonNull(username) || Objects.nonNull(password);
  }
}
