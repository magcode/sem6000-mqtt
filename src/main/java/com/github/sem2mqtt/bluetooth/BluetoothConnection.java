package com.github.sem2mqtt.bluetooth;

import com.coreoz.wisp.Scheduler;

public abstract class BluetoothConnection {

  private BluetoothConnectionManager bluetoothConnectionManager;
  protected Scheduler scheduler;

  public abstract String getMacAddress();

  public BluetoothConnection(BluetoothConnectionManager bluetoothConnectionManager, Scheduler scheduler) {
    this.bluetoothConnectionManager = bluetoothConnectionManager;
    this.scheduler = scheduler;
  }


  public BluetoothConnectionManager getConnectionManager() {
    return bluetoothConnectionManager;
  }
}
