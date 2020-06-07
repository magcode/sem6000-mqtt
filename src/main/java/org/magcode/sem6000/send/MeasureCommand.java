package org.magcode.sem6000.send;

public class MeasureCommand extends Command {

	public MeasureCommand() {
		byte[] message = getMessage("04", "000000");
		setMessage(message);
	}
}