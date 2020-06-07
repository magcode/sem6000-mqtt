package org.magcode.sem6000.receive;

public abstract class SemResponse {
	ResponseType responseType = null;

	public ResponseType getType() {
		return responseType;
	}
}
