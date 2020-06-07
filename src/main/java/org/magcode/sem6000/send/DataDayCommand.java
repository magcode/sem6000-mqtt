package org.magcode.sem6000.send;

public class DataDayCommand extends Command {

	public DataDayCommand() {
		byte[] message = getMessage("0b", "000000");
		setMessage(message);
	}
}