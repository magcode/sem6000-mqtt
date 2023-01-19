package org.magcode.sem6000.connector.receive;

public class AvailabilityResponse extends SemResponse {

	private final Availability availability;

	public enum Availability {
		lost, available
	}

	public AvailabilityResponse(Availability available) {
		super(ResponseType.availability);
		this.availability = available;
	}

	public Availability getAvailability() {
		return availability;
	}

	@Override
	public String toString() {
		return "Device is " + ((availability == Availability.available) ? "online" : "offline");
	}
}