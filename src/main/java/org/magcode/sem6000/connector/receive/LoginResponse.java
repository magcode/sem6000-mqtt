package org.magcode.sem6000.connector.receive;

public class LoginResponse extends SemResponse {

  private final boolean success;

  public LoginResponse(byte b) {
    super(ResponseType.login);
    this.success = b == (byte) 0x00;
  }

  public boolean isSuccess() {
    return success;
  }

  public String toString() {
    if (this.success) {
      return "Login: success";
    } else {
      return "Login: failure";
    }
  }
}