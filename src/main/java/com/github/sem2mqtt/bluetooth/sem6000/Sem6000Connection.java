package com.github.sem2mqtt.bluetooth.sem6000;

import com.github.sem2mqtt.bluetooth.BluetoothConnection;
import com.github.sem2mqtt.configuration.Sem6000Config;

public class Sem6000Connection extends BluetoothConnection {
  private final Sem6000Config sem6000Config;

  public Sem6000Connection(Sem6000Config sem6000Config) {
    this.sem6000Config = sem6000Config;
  }


  @Override
  public String getMacAddress() {
    return sem6000Config.getMac();
  }
}
