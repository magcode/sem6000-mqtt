package org.magcode.sem6000.connector.receive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.magcode.sem6000.connector.ByteUtils;
import org.magcode.sem6000.connector.send.SyncTimeCommand;

class SyncTimeResponseTest {

  @Test
  public void testDate() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    LocalDateTime formatDateTime = LocalDateTime.parse("2020-06-07 10:55:22", formatter);
    SyncTimeCommand syncTimeCommand = new SyncTimeCommand(formatDateTime);
    assertEquals("0f0c010016370a070607e4000051ffff", ByteUtils.byteArrayToHex(syncTimeCommand.getMessage()));
    formatDateTime = LocalDateTime.parse("2020-06-08 09:18:30", formatter);
    syncTimeCommand = new SyncTimeCommand(formatDateTime);
    assertEquals("0f0c01001e1209080607e4000034ffff", ByteUtils.byteArrayToHex(syncTimeCommand.getMessage()));
  }
}
