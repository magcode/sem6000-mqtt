package com.github.sem2mqtt.bluetooth.sem6000;

public enum GattCharacteristic {
  Service("0000fff0-0000-1000-8000-00805f9b34fb"),
  Write("0000fff3-0000-1000-8000-00805f9b34fb"),
  Notify("0000fff4-0000-1000-8000-00805f9b34fb");


  public final String uuid;

  GattCharacteristic(String uuid) {
    this.uuid = uuid;
  }

}
