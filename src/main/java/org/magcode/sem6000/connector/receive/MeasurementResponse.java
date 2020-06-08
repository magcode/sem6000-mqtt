package org.magcode.sem6000.connector.receive;

import java.math.BigInteger;

public class MeasurementResponse extends SemResponse {
	private int voltage;
	private float power;
	private boolean powerOn = false;

	public MeasurementResponse(byte[] data, String id) {
		this.responseType = ResponseType.measure;
		if (data[0] == (byte) 0x01) {
			powerOn = true;
		}
		byte[] cv = new byte[] { data[1], data[2], data[3] };
		this.power = (float) new BigInteger(cv).intValue() / 1000;
		this.voltage = data[4] & 0xFF;
		setId(id);
	}

	public int getVoltage() {
		return voltage;
	}

	public float getPower() {
		return power;
	}

	public String toString() {
		return "[" + this.getId() + "] Measure PowerOn: " + this.powerOn + " Voltage: " + this.voltage + " Power: "
				+ this.power;
	}

	public boolean isPowerOn() {
		return powerOn;
	}
}