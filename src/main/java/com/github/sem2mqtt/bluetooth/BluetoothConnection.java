package com.github.sem2mqtt.bluetooth;

public abstract class BluetoothConnection {
private BluetoothConnectionManager bluetoothConnectionManager;
  public abstract String getMacAddress();

  void setConnectionManager(BluetoothConnectionManager bluetoothConnectionManager) {
    this.bluetoothConnectionManager = bluetoothConnectionManager;
  }

  public BluetoothConnectionManager getConnectionManager() {
    return bluetoothConnectionManager;
  }
}
