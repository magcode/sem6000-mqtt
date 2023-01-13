package com.github.sem2mqtt.bluetooth;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class BluetoothConnectionManager {
  private final Map<String, BluetoothConnection> macAddressToBluetoothConnections;

  public BluetoothConnectionManager() {
    macAddressToBluetoothConnections = new ConcurrentHashMap<>();
  }

  public <T extends BluetoothConnection> T setupConnection(T bluetoothConnection) {
    bluetoothConnection.setConnectionManager(this);
    macAddressToBluetoothConnections.put(bluetoothConnection.getMacAddress(), bluetoothConnection);
    return bluetoothConnection;
  }
}
