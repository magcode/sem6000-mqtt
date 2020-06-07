package org.magcode.sem6000.send;

import java.time.LocalDateTime;

public class SyncTimeCommand extends Command{

	public SyncTimeCommand() {
		this(LocalDateTime.now());
	}

	public SyncTimeCommand(LocalDateTime now) {
		Integer month = now.getMonthValue();
		Integer day = now.getDayOfMonth();
		Integer hour = now.getHour();
		Integer minute = now.getMinute();
		Integer second = now.getSecond();

		byte[] payload = new byte[9];
		payload[0] = second.byteValue();
		payload[1] = minute.byteValue();
		payload[2] = hour.byteValue();
		payload[3] = day.byteValue();
		payload[4] = month.byteValue();
		payload[5] = (byte) ((now.getYear() >> 8) & 0xff);
		payload[6] = (byte) (now.getYear() & 0xFF);
		payload[7] = (byte) 0x00;
		payload[8] = (byte) 0x00;

		byte[] message = getMessage("0100", payload);
		setMessage(message);
	}
}