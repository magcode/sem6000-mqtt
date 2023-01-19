package org.magcode.sem6000.connector.receive;

import org.magcode.sem6000.connector.send.Command;

public class SemResponseTestHelper {

  public static SemResponse createSyncTimeResponse() {
    return createSemResponseFor("0f0401000002ffff");
  }

  public static SemResponse createMeasureResponse() {
    return createSemResponseFor("0f11040001002cc8ea0059320000000000006f");
  }

  public static SemResponse createIncompleteResponse() {
    return createSemResponseFor(
        "0f330a00000000000000000000000000000000000000000000000000000000000000000000000000");
  }

  public static SemResponse createUnknownSemResponse() {
    return createSemResponseFor(
        "0f330a00000700010000000000000000000000000000000000000000000000002effff000b000b00050000000000000000000000000000");
  }

  public static SemResponse createSemDayDataResponse() {
    return createSemResponseFor(
        "0f330a00000000000000000000000000000000000000000000000000005c04e200c9005f0000000000000000000000000000000075ffff");
  }

  public static SemResponse createSemResponseFor(String resp) {
    return SemResponseParser.parseMessage(Command.hexStringToByteArray(resp));
  }
}
