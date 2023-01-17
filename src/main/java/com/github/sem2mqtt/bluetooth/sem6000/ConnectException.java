package com.github.sem2mqtt.bluetooth.sem6000;

public class ConnectException extends Exception {

  public ConnectException(String message) {
    super(message);
  }

  public ConnectException(Exception e) {
    super(e);
  }
}
