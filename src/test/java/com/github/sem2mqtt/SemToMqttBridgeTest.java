package com.github.sem2mqtt;

import static com.github.sem2mqtt.configuration.Sem6000ConfigTestHelper.generateSemConfigs;
import static com.github.sem2mqtt.configuration.Sem6000ConfigTestHelper.randomSemConfigForPlug;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.magcode.sem6000.connector.receive.AvailabilityResponse.Availability.available;
import static org.magcode.sem6000.connector.receive.SemResponseTestHelper.createMeasureResponse;
import static org.magcode.sem6000.connector.receive.SemResponseTestHelper.createSemDayDataResponse;
import static org.magcode.sem6000.connector.receive.SemResponseTestHelper.createUnknownSemResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coreoz.wisp.Scheduler;
import com.github.sem2mqtt.bluetooth.BluetoothConnectionManager;
import com.github.sem2mqtt.bluetooth.sem6000.Sem6000Connection;
import com.github.sem2mqtt.bluetooth.sem6000.Sem6000DbusHandlerProxy.Sem6000ResponseHandler;
import com.github.sem2mqtt.configuration.Sem6000Config;
import com.github.sem2mqtt.mqtt.MqttConnection;
import com.github.sem2mqtt.mqtt.MqttConnection.MessageCallback;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.magcode.sem6000.connector.receive.AvailabilityResponse;
import org.magcode.sem6000.connector.receive.SemResponse;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
class SemToMqttBridgeTest {

  public static final String ROOT_TOPIC = "rootTopic";
  public Scheduler schedulerMock = new Scheduler();
  private MqttConnection mqttConnectionMock;
  private BluetoothConnectionManager bluetoothConnectionManager;
  private Sem6000Connection defaultSem6000ConnectionMock;
  @Captor
  private ArgumentCaptor<Sem6000ResponseHandler> semResponseHandlerCaptor;

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
  @MockitoSettings(strictness = Strictness.LENIENT)
  void fails_on_mqtt_problems_when_running() {
    //given
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(ROOT_TOPIC, generateSemConfigs(1),
        mqttConnectionMock, bluetoothConnectionManager, schedulerMock);
    //when
    doThrow(new RuntimeException()).when(mqttConnectionMock).establish();
    //then
    assertThatCode(semToMqttBridge::run).isInstanceOf(RuntimeException.class);
  }

  @Test
  void subscribes_to_sem6000_messages_when_running() {
    //given
    int countOfSem6000 = 4;
    Set<Sem6000Config> sem6000Configs = generateSemConfigs(countOfSem6000);
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(ROOT_TOPIC, sem6000Configs, mqttConnectionMock,
        bluetoothConnectionManager, schedulerMock);
    Sem6000Connection sem6000ConnectionMock = mock(Sem6000Connection.class);
    when(bluetoothConnectionManager.setupConnection(any())).thenReturn(sem6000ConnectionMock);
    //when
    semToMqttBridge.run();
    //then
    verify(sem6000ConnectionMock, times(countOfSem6000)).subscribe(any());
  }

  @ParameterizedTest
  @MethodSource("sem6000Messages")
  void forwards_sem6000_messages_according_to_message_type_when_retrieving_message(SemResponse semResponse,
      int expectedCountOfMessages) {
    //given
    String plugName = "plug1";
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(ROOT_TOPIC, Set.of(randomSemConfigForPlug(plugName)),
        mqttConnectionMock, bluetoothConnectionManager, schedulerMock);
    Sem6000Connection sem6000ConnectionMock = mock(Sem6000Connection.class);
    when(bluetoothConnectionManager.setupConnection(any())).thenReturn(sem6000ConnectionMock);
    //when
    semToMqttBridge.run();
    verify(sem6000ConnectionMock).subscribe(semResponseHandlerCaptor.capture());
    semResponseHandlerCaptor.getValue().handleSem6000Response(semResponse);
    //then
    verify(mqttConnectionMock, times(expectedCountOfMessages)).publish(
        startsWith(String.format("%s/%s/", ROOT_TOPIC, plugName)), any());
  }

  static Stream<Arguments> sem6000Messages() {
    return Stream.of(Arguments.of(createMeasureResponse(), 3), Arguments.of(createSemDayDataResponse(), 1),
        Arguments.of(new AvailabilityResponse(available), 1), Arguments.of(createUnknownSemResponse(), 0));
  }
}