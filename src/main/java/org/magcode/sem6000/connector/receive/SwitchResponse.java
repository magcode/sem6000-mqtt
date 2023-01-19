package org.magcode.sem6000.connector.receive;

public class SwitchResponse extends SemResponse {

  private final boolean success;

  public SwitchResponse(byte b) {
    super(ResponseType.switchrelay);
    this.success = b == (byte) 0x00;
  }

  public boolean isSuccess() {
    return success;
  }

  public String toString() {
    if (this.success) {
      return "Switch: success";
    } else {
      return "Switch: failure";
    }
  }
}