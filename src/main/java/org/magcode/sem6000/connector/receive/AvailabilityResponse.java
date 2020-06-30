package org.magcode.sem6000.connector.receive;

public class AvailabilityResponse extends SemResponse {
	private Availability availability;

	public enum Availability {
		lost, available
	}

	public AvailabilityResponse(Availability available, String id) {
		this.availability = available;
		this.responseType = ResponseType.availability;
		setId(id);
	}

	public Availability getAvailability() {
		return availability;
	}
}