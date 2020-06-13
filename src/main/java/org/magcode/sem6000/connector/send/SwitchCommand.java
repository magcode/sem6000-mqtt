package org.magcode.sem6000.connector.send;

public class SwitchCommand extends Command {

	public SwitchCommand(boolean on) {
		byte[] payload = new byte[3];
		if (on) {
			payload[0] = (byte) 0x01;
		} else {
			payload[0] = (byte) 0x00;
		}
		payload[1] = (byte) 0x00;
		payload[2] = (byte) 0x00;

		byte[] message = buildMessage("0300", payload);
		setMessage(message);
	}

}
