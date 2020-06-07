package org.magcode.sem6000.receive;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SemResponseParser {
	private static Logger logger = LogManager.getLogger(SemResponseParser.class);

	public static SemResponse parseMessage(byte[] message) {

		if (message[0] == (byte) 0x0f) {
			int expectedLen = message[1] & 0xFF;
			int actualLen = message.length;
			logger.trace("expected: {} actual: {}", expectedLen, actualLen);
			if (expectedLen > actualLen) {
				return new IncompleteResponse(message);
			}
			// login response
			if (message[2] == (byte) 0x17 && message[3] == (byte) 0x00) {
				if (message[4] == (byte) 0x00) {
					return new LoginResponse(true);
				} else {
					return new LoginResponse(false);
				}
			}
			// measurement response
			if (message[2] == (byte) 0x04 && message[3] == (byte) 0x00) {
				int voltage = message[8] & 0xFF;
				return new MeasurementResponse(voltage);
			}
			// synctime response
			if (message[2] == (byte) 0x01 && message[3] == (byte) 0x00) {
				if (message[4] == (byte) 0x00) {
					return new SyncTimeResponse(true);
				} else {
					return new SyncTimeResponse(false);
				}
			}

		}
		return new UnknownResponse();

	}
}
