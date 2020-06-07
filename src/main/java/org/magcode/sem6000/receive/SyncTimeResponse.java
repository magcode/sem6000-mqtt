package org.magcode.sem6000.receive;

public class SyncTimeResponse extends SemResponse {
	private boolean success;

	public SyncTimeResponse(boolean success) {
		this.success = success;
		this.responseType = ResponseType.synctime;
	}

	public boolean isSuccess() {
		return success;
	}

	public String toString() {
		return this.success ? "Synctime: success" : "Synctime: failure";
	}
}