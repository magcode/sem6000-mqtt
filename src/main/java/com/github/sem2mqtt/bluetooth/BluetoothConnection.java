package com.github.sem2mqtt.bluetooth;

import com.coreoz.wisp.Scheduler;

public abstract class BluetoothConnection {

  protected BluetoothConnectionManager connectionManager;
  protected Scheduler scheduler;

  public abstract String getMacAddress();

  public BluetoothConnection(BluetoothConnectionManager bluetoothConnectionManager, Scheduler scheduler) {
    this.connectionManager = bluetoothConnectionManager;
    this.scheduler = scheduler;
  }
}
