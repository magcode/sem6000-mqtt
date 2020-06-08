package org.magcode.sem6000.connector.receive;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.magcode.sem6000.Sem6000MQTT;

public class SemResponseParser {
	private static Logger logger = LogManager.getLogger(SemResponseParser.class);

	public static SemResponse parseMessage(byte[] message, String id) {
		logger.trace("Parsing {}", Sem6000MQTT.byteArrayToHex(message));
		if (message[0] == (byte) 0x0f) {
			int expectedLen = message[1] & 0xFF;
			int actualLen = message.length;
			logger.trace("Lenght expected: {} actual: {}", expectedLen, actualLen);
			if (!(actualLen - expectedLen - 4 == 0 || actualLen - expectedLen - 2 == 0)) {
				return new IncompleteResponse(message);
			}
			// login response
			if (message[2] == (byte) 0x17 && message[3] == (byte) 0x00) {
				return new LoginResponse(message[4],id);
			}
			// measurement response
			if (message[2] == (byte) 0x04 && message[3] == (byte) 0x00) {
				byte[] data = new byte[48];
				System.arraycopy(message, 4, data, 0, 14);
				return new MeasurementResponse(data,id);
			}
			// data day response
			if (message[2] == (byte) 0x0a && message[3] == (byte) 0x00) {
				byte[] data = new byte[48];
				System.arraycopy(message, 4, data, 0, 48);
				return new DataDayResponse(data,id);
			}
			// synctime response
			if (message[2] == (byte) 0x01 && message[3] == (byte) 0x00) {
				return new SyncTimeResponse(message[4],id);
			}
			return new UnknownResponse();

		}
		return new IncompleteResponse(message);
	}
}
