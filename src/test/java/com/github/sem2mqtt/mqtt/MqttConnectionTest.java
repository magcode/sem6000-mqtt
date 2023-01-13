package com.github.sem2mqtt.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.github.sem2mqtt.configuration.MqttConfig;
import com.github.sem2mqtt.mqtt.MqttConnection.MessageCallback;
import java.util.concurrent.ThreadLocalRandom;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

class MqttConnectionTest {

  public static final String MQTT_PASSWORD = "password";
  public static final String MQTT_USERNAME = "user";
  public static final String MQTT_CLIENT_ID = "mqtt-client-id";
  private final MqttClient mqttClientMock = Mockito.mock(MqttClient.class);
  private MqttConnection mqttConnection;

  @BeforeEach
  void setUp() {
    mqttConnection = new MqttConnection(
        mqttClientMock,
        new MqttConfig("rootTopic", "tcp://some-url", MQTT_CLIENT_ID, MQTT_USERNAME, MQTT_PASSWORD));
  }

  @Test
  void connects_to_mqtt_server_when_establishing_connection() throws MqttException {
    //when
    mqttConnection.establish();
    //then
    verify(mqttClientMock).connect(argThat(mqttConnectOptions -> {
      assertThat(mqttConnectOptions.getUserName()).isEqualTo(MQTT_USERNAME);
      assertThat(mqttConnectOptions.getPassword()).isEqualTo(MQTT_PASSWORD.toCharArray());
      return true;
    }));
  }

  @Test
  void enables_auto_reconnect_when_establishing_connection() throws MqttException {
    //when
    mqttConnection.establish();
    //then
    verify(mqttClientMock).connect(argThat(MqttConnectOptions::isAutomaticReconnect));
  }

  @Test
  void subscribes_to_topics_when_requesting_subscription() throws MqttException {
    //given
    String topic = "measurements/plug2/+/set";
    //when
    mqttConnection.subscribe(topic, mock(MessageCallback.class));
    //then
    verify(mqttClientMock).subscribe(eq(topic), any(IMqttMessageListener.class));
  }

  @Test
  void invokes_callback_when_message_for_subscribed_topic_arrives() throws MqttException {
    //given
    String topic = "measurements/plug2/+/set";
    MessageCallback callback = mock(MessageCallback.class);
    //when
    mqttConnection.subscribe(topic, callback);
    //then
    verify(mqttClientMock).subscribe(eq(topic), argThat(whenCalledForwardsToCallback(callback)));
  }

  private ArgumentMatcher<IMqttMessageListener> whenCalledForwardsToCallback(MessageCallback callback) {
    return listener -> {
      String messageTopic = "measurements/plug2/led/set";
      MqttMessage message = randomMessage();
      try {
        listener.messageArrived(messageTopic, message);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      verify(callback).handleMqttMessage(messageTopic, message);
      return true;
    };
  }

  private MqttMessage randomMessage() {
    byte[] randomMessage = new byte[128];
    ThreadLocalRandom.current().nextBytes(randomMessage);
    return new MqttMessage(randomMessage);
  }

}