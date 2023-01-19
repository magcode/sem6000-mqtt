package com.github.sem2mqtt.bluetooth.sem6000;

import static com.github.sem2mqtt.bluetooth.sem6000.Sem6000DbusMessageTestHelper.createMeasurementPropertyChange;
import static com.github.sem2mqtt.configuration.Sem6000ConfigTestHelper.randomSemConfigForPlug;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_MOCKS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coreoz.wisp.Scheduler;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattService;
import com.github.sem2mqtt.bluetooth.BluetoothConnectionManager;
import com.github.sem2mqtt.bluetooth.DevicePropertiesChangedHandler.DbusListener;
import com.github.sem2mqtt.bluetooth.sem6000.Sem6000DbusHandlerProxy.Sem6000ResponseHandler;
import com.github.sem2mqtt.configuration.Sem6000Config;
import java.time.Duration;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezInvalidValueLengthException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotPermittedException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.exceptions.DBusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
class Sem6000ConnectionTest {

  private final Scheduler scheduler = new Scheduler();
  private BluetoothDevice sem6000DeviceMock;
  private BluetoothGattCharacteristic writeService;
  private BluetoothGattCharacteristic notifyService;
  private BluetoothConnectionManager bluetoothConnectionManagerMock;
  @Captor
  ArgumentCaptor<DbusListener> dbusListenerCaptor;

  /**
   * Set up a connection manager with a device that is connected and returns sem6000 gatt service and its write and
   * notify services.
   */
  @BeforeEach
  void setUp() throws Exception {
    bluetoothConnectionManagerMock = mock(BluetoothConnectionManager.class);
    sem6000DeviceMock = mock(BluetoothDevice.class, RETURNS_MOCKS);
    when(sem6000DeviceMock.connect()).thenReturn(true);
    when(sem6000DeviceMock.isConnected()).thenReturn(true);
    when(bluetoothConnectionManagerMock.findDeviceOrFail(anyString(), any())).thenReturn(sem6000DeviceMock);
    BluetoothGattService gattService = mock(BluetoothGattService.class, RETURNS_MOCKS);
    when(sem6000DeviceMock.getGattServiceByUuid(Sem6000GattCharacteristic.Service.uuid)).thenReturn(gattService);
    writeService = mock(BluetoothGattCharacteristic.class, RETURNS_MOCKS);
    when(gattService.getGattCharacteristicByUuid(Sem6000GattCharacteristic.Write.uuid)).thenReturn(writeService);
    notifyService = mock(BluetoothGattCharacteristic.class, RETURNS_MOCKS);
    when(gattService.getGattCharacteristicByUuid(Sem6000GattCharacteristic.Notify.uuid)).thenReturn(notifyService);
  }

  @Test
  @MockitoSettings(strictness = Strictness.LENIENT)
  void mac_matches_from_config_when_creating_connection() {
    //given
    Sem6000Config sem6000Config = randomSemConfigForPlug("plug1");
    //when
    Sem6000Connection sem6000Connection = new Sem6000Connection(sem6000Config,
        new BluetoothConnectionManager(), scheduler);
    //then
    assertThat(sem6000Connection.getMacAddress()).isEqualTo(sem6000Connection.getMacAddress());
  }

  @Test
  void connects_to_bluetooth_device_when_establishing_connection() {
    //given
    Sem6000Connection sem6000Connection = new Sem6000Connection(randomSemConfigForPlug("plug1"),
        bluetoothConnectionManagerMock, scheduler);
    bluetoothConnectionManagerMock.setupConnection(sem6000Connection);
    //when
    sem6000Connection.establish();
    //then
    verify(sem6000DeviceMock).connect();
  }

  @Test
  void sends_login_and_sync_time_command_when_establishing_connection()
      throws BluezFailedException, BluezNotAuthorizedException, BluezInvalidValueLengthException, BluezNotSupportedException, BluezInProgressException, BluezNotPermittedException {
    //given
    Sem6000Connection sem6000Connection = new Sem6000Connection(randomSemConfigForPlug("plug1"),
        bluetoothConnectionManagerMock, scheduler);
    bluetoothConnectionManagerMock.setupConnection(sem6000Connection);
    //when
    sem6000Connection.establish();
    //then
    verify(writeService, times(2)).writeValue(any(), anyMap());
  }

  @Test
  void connection_is_established_when_establishing_connection() {
    //given
    Sem6000Connection sem6000Connection = new Sem6000Connection(randomSemConfigForPlug("plug1"),
        bluetoothConnectionManagerMock, scheduler);
    bluetoothConnectionManagerMock.setupConnection(sem6000Connection);
    //when
    sem6000Connection.establish();
    //then
    assertThat(sem6000Connection.isEstablished()).isTrue();
  }

  @Test
  void sends_measurement_and_day_requests_when_connection_is_established()
      throws InterruptedException, BluezFailedException, BluezNotAuthorizedException, BluezInvalidValueLengthException, BluezNotSupportedException, BluezInProgressException, BluezNotPermittedException {
    //given
    Duration updateInterval = Duration.ofMillis(10);
    Sem6000Connection sem6000Connection = new Sem6000Connection(randomSemConfigForPlug("plug1", updateInterval),
        bluetoothConnectionManagerMock, scheduler);
    sem6000Connection.establish();
    reset(writeService); // reset to ignore e.g. the login message
    //when
    // wait 1.5 times the update interval, because of the message processing overhead
    Thread.sleep(Math.round(updateInterval.toMillis() * 1.5));
    //then
    verify(writeService, Mockito.times(2)).writeValue(any(), anyMap());
  }

  @Test
  void sends_measurement_requests_regularly_when_connection_is_established()
      throws InterruptedException, BluezFailedException, BluezNotAuthorizedException, BluezInvalidValueLengthException, BluezNotSupportedException, BluezInProgressException, BluezNotPermittedException {
    //given
    Duration updateInterval = Duration.ofMillis(15);
    Sem6000Connection sem6000Connection = new Sem6000Connection(randomSemConfigForPlug("plug1", updateInterval),
        bluetoothConnectionManagerMock, scheduler);
    sem6000Connection.establish();
    reset(writeService); // reset to ignore e.g. the login message
    //when
    Thread.sleep(Math.round(updateInterval.toMillis() * 7));
    //then
    // there is an overhead when processing the message, therefore,
    // although we wait 7 times the update interval,
    // we only expect at least 5 times 2 (one for measures and one for data day) requests
    verify(writeService, atLeast(10)).writeValue(any(), anyMap());
  }

  @Test
  void forwards_bluetooth_messages_when_subscribed_for_messages()
      throws DBusException {
    //given
    Sem6000Connection sem6000Connection = new Sem6000Connection(randomSemConfigForPlug("plug1", Duration.ofSeconds(60)),
        bluetoothConnectionManagerMock, scheduler);
    String dbusPath = "/org/bluez/bt1/dev_00_00_00_00_00_01/service000e/char0013";
    when(notifyService.getDbusPath()).thenReturn(dbusPath);
    sem6000Connection.establish();
    Sem6000ResponseHandler responseHandler = mock(Sem6000ResponseHandler.class);
    verify(bluetoothConnectionManagerMock).subscribeToDbusPath(eq(dbusPath), dbusListenerCaptor.capture());
    //when
    sem6000Connection.subscribe(responseHandler);
    dbusListenerCaptor.getValue().handle(createMeasurementPropertyChange(dbusPath));
    //then
    verify(responseHandler, atLeastOnce()).handleSem6000Response(any());
  }

}