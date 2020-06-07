package org.magcode.sem6000.receive;

import java.math.BigInteger;
import java.nio.ByteBuffer;

public class DataDayResponse extends SemResponse {
	private int en24h;

	public DataDayResponse(byte[] values) {
		this.responseType = ResponseType.dataday;
		int total = 0;
		for (int i = 0; i < 24; i=i+1) {
			byte[] cv = new byte[] { values[i], values[i + 1] };

			total = total + new BigInteger(cv).intValue();
		}
	}

	public String toString() {
		return "24h: " + en24h;
	}

	public int getLast24h() {
		return en24h;
	}

}