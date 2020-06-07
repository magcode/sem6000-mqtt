package org.magcode.sem6000.send;

import org.magcode.sem6000.Sem6BleApp;

public class LoginCommand extends Command {

	public LoginCommand(String pin) {
		char[] pinA = pin.toCharArray();
		Integer pos1 = Integer.valueOf(pinA[0]+"");
		Integer pos2 = Integer.valueOf(pinA[1]+"");
		Integer pos3 = Integer.valueOf(pinA[2]+"");
		Integer pos4 = Integer.valueOf(pinA[3]+"");
		byte[] message = Sem6BleApp.getMessage("1700",
				"00" + pos1.byteValue() + pos2.byteValue() + pos3.byteValue() + pos4.byteValue() + "00000000");
		setMessage(message);
	}

}
