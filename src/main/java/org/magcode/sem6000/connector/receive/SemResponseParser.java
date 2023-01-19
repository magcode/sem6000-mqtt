package org.magcode.sem6000.connector.receive;

import org.magcode.sem6000.connector.ByteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemResponseParser {

  private static final Logger logger = LoggerFactory.getLogger(SemResponseParser.class);

  public static SemResponse parseMessage(byte[] message) {
    logger.debug("Parsing {}", ByteUtils.byteArrayToHex(message));
    if (message[0] == (byte) 0x0f) {
      int expectedLen = message[1] & 0xFF;
      int actualLen = message.length;
      logger.debug("Length expected: {} actual: {}", expectedLen, actualLen);
      if (!(actualLen - expectedLen - 4 == 0 || actualLen - expectedLen - 2 == 0)) {
        logger.debug("It's not complete");
        return new IncompleteResponse(message);
      }
      // login response
      if (message[2] == (byte) 0x17 && message[3] == (byte) 0x00) {
        return new LoginResponse(message[4]);
      }
      // measurement response
      if (message[2] == (byte) 0x04 && message[3] == (byte) 0x00) {
        byte[] data = new byte[48];
        System.arraycopy(message, 4, data, 0, 14);
        return new MeasurementResponse(data);
      }
      // data day response
      if (actualLen == 55 && message[2] == (byte) 0x0a && message[3] == (byte) 0x00
          && message[actualLen - 1] == (byte) 0xff) {
        byte[] data = new byte[48];
        System.arraycopy(message, 4, data, 0, 48);
        return new DataDayResponse(data);
      }
      // synctime response
      if (message[2] == (byte) 0x01 && message[3] == (byte) 0x00) {
        return new SyncTimeResponse(message[4]);
      }
      // switch response
      if (message[2] == (byte) 0x03 && message[3] == (byte) 0x00) {
        return new SwitchResponse(message[4]);
      }
      // led response
      if (message[2] == (byte) 0x0f && message[3] == (byte) 0x00) {
        return new LedResponse();
      }
      return new UnknownResponse();

    }
    return new IncompleteResponse(message);
  }
}
