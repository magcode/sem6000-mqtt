package org.magcode.sem6000.connector.send;

public class LoginCommand extends Command {

	public LoginCommand(String pin) {
		char[] pinA = pin.toCharArray();
		Integer pos1 = Integer.valueOf(pinA[0] + "");
		Integer pos2 = Integer.valueOf(pinA[1] + "");
		Integer pos3 = Integer.valueOf(pinA[2] + "");
		Integer pos4 = Integer.valueOf(pinA[3] + "");
		byte[] payload = new byte[9];
		payload[0] = (byte) 0x00;
		payload[1] = pos1.byteValue();
		payload[2] = pos2.byteValue();
		payload[3] = pos3.byteValue();
		payload[4] = pos4.byteValue();
		payload[5] = (byte) 0x00;
		payload[6] = (byte) 0x00;
		payload[7] = (byte) 0x00;
		payload[8] = (byte) 0x00;

		byte[] message = buildMessage("1700", payload);

		setMessage(message);
	}

}
