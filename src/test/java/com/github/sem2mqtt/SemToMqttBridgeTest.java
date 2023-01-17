package com.github.sem2mqtt;

import static com.github.sem2mqtt.configuration.Sem6000ConfigHelper.generateSemConfigs;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coreoz.wisp.Scheduler;
import com.github.sem2mqtt.bluetooth.BluetoothConnectionManager;
import com.github.sem2mqtt.bluetooth.sem6000.Sem6000Connection;
import com.github.sem2mqtt.configuration.Sem6000Config;
import com.github.sem2mqtt.mqtt.MqttConnection;
import com.github.sem2mqtt.mqtt.MqttConnection.MessageCallback;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SemToMqttBridgeTest {

  public static final String ROOT_TOPIC = "rootTopic";
  public Scheduler schedulerMock = new Scheduler();
  private MqttConnection mqttConnectionMock;
  private BluetoothConnectionManager bluetoothConnectionManager;
  private Sem6000Connection defaultSem6000ConnectionMock;

  @BeforeEach
  void setUp() {
    mqttConnectionMock = mock(MqttConnection.class);
    bluetoothConnectionManager = mock(BluetoothConnectionManager.class);

    defaultSem6000ConnectionMock = mock(Sem6000Connection.class);
    when(bluetoothConnectionManager.setupConnection(any())).thenReturn(defaultSem6000ConnectionMock);
  }

  @Test
  void establishes_mqtt_connection_when_running() {
    //given
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(ROOT_TOPIC, generateSemConfigs(1),
        mqttConnectionMock, bluetoothConnectionManager, schedulerMock);
    //when
    semToMqttBridge.run();
    //then
    verify(mqttConnectionMock).establish();
  }

  @Test
  void subscribes_to_mqtt_setter_of_each_sem6000_when_running() {
    //given
    Set<Sem6000Config> sem6000Configs = generateSemConfigs(4);
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(ROOT_TOPIC, sem6000Configs, mqttConnectionMock,
        bluetoothConnectionManager, schedulerMock);
    //when
    semToMqttBridge.run();
    //then
    for (Sem6000Config sem6000Config : sem6000Configs) {
      verify(mqttConnectionMock).subscribe(
          matches(String.format("^%s\\/%s\\/\\+\\/set$", ROOT_TOPIC, sem6000Config.getName())),
          any(MessageCallback.class));
    }
  }

  @Test
  void establishes_a_bluetooth_connection_to_each_sem6000_when_running() {
    //given
    int countOfSem6000 = 4;
    Set<Sem6000Config> sem6000Configs = generateSemConfigs(countOfSem6000);
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(ROOT_TOPIC, sem6000Configs, mqttConnectionMock,
        bluetoothConnectionManager, schedulerMock);
    //when
    semToMqttBridge.run();
    //then
    for (Sem6000Config sem6000Config : sem6000Configs) {
      verify(bluetoothConnectionManager).setupConnection(
          argThat(bluetoothConnection -> bluetoothConnection.getMacAddress().equals(sem6000Config.getMac())));
    }
    verify(defaultSem6000ConnectionMock, times(countOfSem6000)).establish();
  }

  @Test
  void fails_on_mqtt_problems_when_running() {
    //given
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(ROOT_TOPIC, generateSemConfigs(1),
        mqttConnectionMock, bluetoothConnectionManager, schedulerMock);
    //when
    doThrow(new RuntimeException()).when(mqttConnectionMock).establish();
    //then
    assertThatCode(semToMqttBridge::run).isInstanceOf(RuntimeException.class);
  }
}