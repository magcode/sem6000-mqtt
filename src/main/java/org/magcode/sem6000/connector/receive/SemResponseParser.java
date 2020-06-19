package org.magcode.sem6000.connector.receive;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.magcode.sem6000.connector.ByteUtils;

public class SemResponseParser {
	private static Logger logger = LogManager.getLogger(SemResponseParser.class);

	public static SemResponse parseMessage(byte[] message, String id) {
		logger.trace("Parsing {}", ByteUtils.byteArrayToHex(message));
		if (message[0] == (byte) 0x0f) {
			int expectedLen = message[1] & 0xFF;
			int actualLen = message.length;
			logger.trace("Lenght expected: {} actual: {}", expectedLen, actualLen);
			if (!(actualLen - expectedLen - 4 == 0 || actualLen - expectedLen - 2 == 0)) {
				logger.trace("Its not complete");
				return new IncompleteResponse(message);
			}
			// login response
			if (message[2] == (byte) 0x17 && message[3] == (byte) 0x00) {
				return new LoginResponse(message[4], id);
			}
			// measurement response
			if (message[2] == (byte) 0x04 && message[3] == (byte) 0x00) {
				byte[] data = new byte[48];
				System.arraycopy(message, 4, data, 0, 14);
				return new MeasurementResponse(data, id);
			}
			// data day response
			if (actualLen == 55 && message[2] == (byte) 0x0a && message[3] == (byte) 0x00 && message[actualLen - 1] == (byte) 0xff) {
				byte[] data = new byte[48];
				System.arraycopy(message, 4, data, 0, 48);
				return new DataDayResponse(data, id);
			}
			// synctime response
			if (message[2] == (byte) 0x01 && message[3] == (byte) 0x00) {
				return new SyncTimeResponse(message[4], id);
			}
			// switch response
			if (message[2] == (byte) 0x03 && message[3] == (byte) 0x00) {
				return new SwitchResponse(message[4], id);
			}
			// led response
			if (message[2] == (byte) 0x0f && message[3] == (byte) 0x00) {
				return new LedResponse(id);
			}
			return new UnknownResponse();

		}
		return new IncompleteResponse(message);
	}
}
