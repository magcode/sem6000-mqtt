package org.magcode.sem6000.connector.receive;

public class LedResponse extends SemResponse {

  public LedResponse() {
    super(ResponseType.led);
  }

  public String toString() {
    return "LED: success";
  }
}