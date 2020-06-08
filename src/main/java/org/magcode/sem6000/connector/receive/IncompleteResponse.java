package org.magcode.sem6000.connector.receive;

public class IncompleteResponse extends SemResponse {
	private byte[] data;

	public IncompleteResponse(byte[] message) {
		this.responseType = ResponseType.incomplete;
		this.data = message;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
}