package org.magcode.sem6000.connector.receive;

public class LedResponse extends SemResponse {
	public LedResponse(String id) {
		this.responseType = ResponseType.led;
		this.setId(id);
	}

	public String toString() {
		return "[" + this.getId() + "] LED: success";
	}
}