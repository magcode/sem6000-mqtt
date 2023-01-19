package org.magcode.sem6000.connector.receive;

import java.math.BigInteger;
import java.time.LocalDateTime;

public class DataDayResponse extends SemResponse {

  private final int en24h;
  private final int enToday;

  public DataDayResponse(byte[] values) {
    super(ResponseType.dataday);
    LocalDateTime now = LocalDateTime.now();
    int total = 0;
    int today = 0;
    for (int i = 0; i < 24; i = i + 1) {
      byte[] cv = new byte[]{values[i * 2], values[(i * 2) + 1]};
      LocalDateTime timeOfValue = LocalDateTime.now().plusHours(-23 + i).withMinute(0).withSecond(0).withNano(0);

      int hourVal = new BigInteger(cv).intValue();
      if (now.getDayOfMonth() == timeOfValue.getDayOfMonth()) {
        today = today + hourVal;
      }
      total = total + hourVal;
    }
    this.en24h = total;
    this.enToday = today;
  }

	public String toString() {
    return "DataDay Energy last 24h: " + en24h + " Wh, Energy today: " + enToday + " Wh";
	}

	public int getLast24h() {
		return en24h;
	}

	public int getToday() {
		return enToday;
	}

}