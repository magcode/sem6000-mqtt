package com.github.sem2mqtt.bluetooth;

import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.sem2mqtt.bluetooth.DevicePropertiesChangedHandler.DbusListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.freedesktop.dbus.exceptions.DBusException;

public class BluetoothConnectionManager {

  private final Map<String, BluetoothConnection> macAddressToBluetoothConnections;
  private DeviceManager deviceManager;
  private DevicePropertiesChangedHandler dbusPathHandler;

  public BluetoothConnectionManager() {
    macAddressToBluetoothConnections = new ConcurrentHashMap<>();
  }

  public void init() {

    try {
      deviceManager = DeviceManager.createInstance(false);
      dbusPathHandler = new DevicePropertiesChangedHandler();
      deviceManager.registerPropertyHandler(dbusPathHandler);
    } catch (DBusException e) {
      throw new RuntimeException("Failed to initialize bluetooth device manager", e);
    }
    deviceManager.scanForBluetoothDevices(10 * 1000);
  }

  public <T extends BluetoothConnection> T setupConnection(T bluetoothConnection) {
    macAddressToBluetoothConnections.put(bluetoothConnection.getMacAddress(), bluetoothConnection);
    return bluetoothConnection;
  }

  public <T extends Exception> BluetoothDevice findDeviceOrFail(String macAddress, T e) throws T {
    return deviceManager.getDevices().stream()
        .filter(bluetoothDevice -> bluetoothDevice.getAddress().equals(macAddress)).findFirst().orElseThrow(() -> e);
  }

  public void subscribeToDbusPath(String dbusPath, DbusListener listener) {
    dbusPathHandler.subscribe(dbusPath, listener);
  }

  public void ignoreDbusPath(String dbusPath) {
    dbusPathHandler.ignore(dbusPath);
  }
}
