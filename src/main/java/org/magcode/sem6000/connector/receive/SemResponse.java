package org.magcode.sem6000.connector.receive;

public abstract class SemResponse {
	ResponseType responseType = null;
	private String id = "";

	public ResponseType getType() {
		return responseType;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
