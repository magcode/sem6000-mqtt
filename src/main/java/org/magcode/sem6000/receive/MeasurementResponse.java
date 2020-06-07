package org.magcode.sem6000.receive;

public class MeasurementResponse extends SemResponse {
	private int voltage;

	public MeasurementResponse(int voltage) {
		this.voltage = voltage;
		this.responseType = ResponseType.measure;
	}

	public int getVoltage() {
		return voltage;
	}

	public String toString() {
		return "Voltage: " + this.voltage;
	}

}