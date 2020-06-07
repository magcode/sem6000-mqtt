package org.magcode.sem6000.send;

import org.magcode.sem6000.Sem6BleApp;

public class MeasureCommand extends Command {

	public MeasureCommand() {
		byte[] message = Sem6BleApp.getMessage("04", "000000");
		setMessage(message);
	}
}