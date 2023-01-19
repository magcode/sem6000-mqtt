package org.magcode.sem6000.connector.receive;

public class SyncTimeResponse extends SemResponse {

  private final boolean success;

  public SyncTimeResponse(byte b) {
    super(ResponseType.synctime);
    this.success = b == (byte) 0x00;
  }

  public boolean isSuccess() {
    return success;
  }

  public String toString() {
    if (this.success) {
      return "Synctime: success";
    } else {
      return "Synctime: failure";
    }
  }
}