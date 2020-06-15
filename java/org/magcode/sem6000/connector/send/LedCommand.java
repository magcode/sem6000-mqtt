package org.magcode.sem6000.connector.send;

public class LedCommand extends Command {

	public LedCommand(boolean on) {
		byte[] payload = new byte[6];
		payload[0] = (byte) 0x05;
		if (on) {
			payload[1] = (byte) 0x01;
		} else {
			payload[1] = (byte) 0x00;
		}
		payload[2] = (byte) 0x00;
		payload[3] = (byte) 0x00;
		payload[4] = (byte) 0x00;
		payload[5] = (byte) 0x00;

		byte[] message = buildMessage("0f00", payload);
		setMessage(message);
	}

}
