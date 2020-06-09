package org.magcode.sem6000.connector.receive;

import java.math.BigInteger;
import java.time.LocalDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataDayResponse extends SemResponse {
	private int en24h;
	private int enToday;
	private static Logger logger = LogManager.getLogger(DataDayResponse.class);

	public DataDayResponse(byte[] values, String id) {
		this.responseType = ResponseType.dataday;
		LocalDateTime now = LocalDateTime.now();
		int total = 0;
		int today = 0;
		for (int i = 0; i < 24; i = i + 1) {
			byte[] cv = new byte[] { values[i * 2], values[(i * 2) + 1] };
			LocalDateTime timeOfValue = LocalDateTime.now().plusHours(-23 + i).withMinute(0).withSecond(0).withNano(0);

			int hourVal = new BigInteger(cv).intValue();
			logger.trace("Data at {} : {} Wh", timeOfValue, hourVal);
			if (now.getDayOfMonth() == timeOfValue.getDayOfMonth()) {
				today = today + hourVal;
			}
			total = total + hourVal;
		}
		this.en24h = total;
		this.enToday = today;
		setId(id);
	}

	public String toString() {
		return "[" + this.getId() + "] DataDay Energy last 24h: " + en24h + " Wh, Energy today: " + enToday + " Wh";
	}

	public int getLast24h() {
		return en24h;
	}

	public int getToday() {
		return enToday;
	}

}