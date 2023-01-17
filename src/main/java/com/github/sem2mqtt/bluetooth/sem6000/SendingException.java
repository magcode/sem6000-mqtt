package com.github.sem2mqtt.bluetooth.sem6000;

public class SendingException extends Exception {

  public SendingException(String message) {
    super(message);
  }

  public SendingException(String message, Exception e) {
    super(message, e);
  }
}
