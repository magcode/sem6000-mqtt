package com.github.sem2mqtt;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.github.sem2mqtt.configuration.Sem6000Config;
import com.github.sem2mqtt.mqtt.MqttConnection;
import com.github.sem2mqtt.mqtt.MqttConnection.MessageCallback;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SemToMqttBridgeTest {

  public static final String ROOT_TOPIC = "rootTopic";
  private MqttConnection mqttConnection;

  @BeforeEach
  void setUp() {
    mqttConnection = mock(MqttConnection.class);
  }


  private Sem6000Config randomSemConfigForPlug(String plugName) {
    return new Sem6000Config(randomMac(), randomPin(), plugName, Duration.ofSeconds(60));
  }

  @Test
  void establishes_mqtt_connection_when_running() {
    //given
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(mqttConnection, ROOT_TOPIC,
        generateBridgeConfigForNSem6000(1));
    //when
    semToMqttBridge.run();
    //then
    verify(mqttConnection).establish();
  }

  @Test
  void subscribes_to_setter_of_each_sem6000_when_running() {
    //given
    Set<Sem6000Config> sem6000Configs = generateBridgeConfigForNSem6000(4);
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(mqttConnection, ROOT_TOPIC,
        sem6000Configs);
    //when
    semToMqttBridge.run();
    //then
    for (Sem6000Config sem6000Config : sem6000Configs) {
      verify(mqttConnection).subscribe(
          matches(String.format("^%s\\/%s\\/\\+\\/set$", ROOT_TOPIC, sem6000Config.getName())),
          any(MessageCallback.class));
    }
  }

  @Test
  void fails_on_mqtt_problems_when_running() {
    //given
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(mqttConnection, ROOT_TOPIC,
        generateBridgeConfigForNSem6000(1));
    //when
    doThrow(new RuntimeException()).when(mqttConnection).establish();
    //then
    assertThatCode(semToMqttBridge::run).isInstanceOf(RuntimeException.class);
  }

  private Set<Sem6000Config> generateBridgeConfigForNSem6000(int countOfSems) {
    return
        IntStream.range(0, countOfSems)
            .mapToObj(num -> randomSemConfigForPlug("plug" + num))
            .collect(toSet());
  }

  private String randomPin() {
    return String.valueOf(ThreadLocalRandom.current().nextInt(100000));
  }

  private String randomMac() {
    return ThreadLocalRandom.current().ints()
        .limit(6)
        .map(num -> num % 100)
        .map(Math::abs)
        .mapToObj(String::valueOf)
        .collect(joining(":"));
  }
}