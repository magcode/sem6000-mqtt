package org.magcode.sem6000.connector.receive;

public abstract class SemResponse {
	ResponseType responseType = null;

	public ResponseType getType() {
		return responseType;
	}
}
