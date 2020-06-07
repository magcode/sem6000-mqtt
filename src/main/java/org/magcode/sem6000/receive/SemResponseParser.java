package org.magcode.sem6000.receive;

public class SemResponseParser {
	public static SemResponse parseMessage(byte[] message) {

		if (message[0] == (byte) 0x0f) {
			int len = message[1] & 0xFF;
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

		}
		return new UnknownResponse();

	}
}
