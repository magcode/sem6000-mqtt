package com.github.sem2mqtt.bluetooth.sem6000;

import static com.github.sem2mqtt.configuration.Sem6000ConfigHelper.randomSemConfigForPlug;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_MOCKS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coreoz.wisp.Scheduler;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattService;
import com.github.sem2mqtt.bluetooth.BluetoothConnectionManager;
import com.github.sem2mqtt.configuration.Sem6000Config;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezInvalidValueLengthException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotPermittedException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Sem6000ConnectionTest {

  private final Scheduler scheduler = new Scheduler();
  private BluetoothDevice sem6000DeviceMock;
  private BluetoothGattService gattService;
  private BluetoothGattCharacteristic writeService;
  private BluetoothGattCharacteristic notifyService;
  private BluetoothConnectionManager bluetoothConnectionManagerMock;

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
    gattService = mock(BluetoothGattService.class, RETURNS_MOCKS);
    when(sem6000DeviceMock.getGattServiceByUuid(Sem6000GattCharacteristic.Service.uuid)).thenReturn(gattService);
    writeService = mock(BluetoothGattCharacteristic.class, RETURNS_MOCKS);
    when(gattService.getGattCharacteristicByUuid(Sem6000GattCharacteristic.Write.uuid)).thenReturn(writeService);
    notifyService = mock(BluetoothGattCharacteristic.class, RETURNS_MOCKS);
    when(gattService.getGattCharacteristicByUuid(Sem6000GattCharacteristic.Notify.uuid)).thenReturn(notifyService);
  }

  @Test
  void mac_matches_from_config_when_creating_connection() {
    //given
    Sem6000Config sem6000Config = randomSemConfigForPlug("plug1");
    //when
    Sem6000Connection sem6000Connection = new Sem6000Connection(sem6000Config,
        new BluetoothConnectionManager(scheduler), scheduler);
    //then
    assertThat(sem6000Connection.getMacAddress()).isEqualTo(sem6000Connection.getMacAddress());
  }

  @Test
  void connects_to_bluetooth_device_when_establishing_connection()
      throws BluezFailedException, BluezNotAuthorizedException, BluezInvalidValueLengthException, BluezNotSupportedException, BluezInProgressException, BluezNotPermittedException {
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
}