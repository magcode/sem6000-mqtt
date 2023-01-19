package com.github.sem2mqtt.mqtt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sem6000MqttTopic {

  public enum Type {
    relay, unknown, led
  }

  private final String rootTopic;
  private final String topic;
  private final String sem6000PlugName;

  public Sem6000MqttTopic(String rootTopic, String topic, String sem6000PlugName) {
    this.rootTopic = rootTopic;
    this.topic = topic;
    this.sem6000PlugName = sem6000PlugName;
  }

  public Type getType() {
    Matcher matcher = Pattern.compile(getMatcherForValidTopics()).matcher(topic);
    if (matcher.matches()) {
      return Type.valueOf(matcher.group("type"));
    } else {
      return Type.unknown;
    }
  }

  public boolean isValid() {
    return topic.matches(getMatcherForValidTopics());
  }

  private String getMatcherForValidTopics() {
    return String.format("^%s\\/%s\\/(?<type>relay|led)$", rootTopic, sem6000PlugName);
  }

  public static String createSetterSubscription(String rootTopic, String name) {
    return String.format("%s/%s/+/set", rootTopic, name);
  }

  @Override
  public String toString() {
    return topic;
  }
}
