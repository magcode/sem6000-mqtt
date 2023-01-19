package org.magcode.sem6000.connector.receive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.magcode.sem6000.connector.ByteUtils;
import org.magcode.sem6000.connector.send.LoginCommand;

class LoginResponseTest {

  @Test
  public void testLogin() {
    LoginCommand loginCommand = new LoginCommand("1234");
    assertEquals("0f0c170000010203040000000022ffff", ByteUtils.byteArrayToHex(loginCommand.getMessage()));
  }
}