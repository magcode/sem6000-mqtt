package org.magcode.sem6000.connector.send;

public class LedCommand extends Command {

	private final boolean state;

	public LedCommand(boolean state) {
		this.state = state;
		byte[] payload = new byte[6];
		payload[0] = (byte) 0x05;
		if (state) {
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

	@Override
	public String toString() {
		return String.format("led %s", state ? "on" : "off");
	}
}
