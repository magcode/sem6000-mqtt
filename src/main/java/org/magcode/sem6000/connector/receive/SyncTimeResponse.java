package org.magcode.sem6000.connector.receive;

public class SyncTimeResponse extends SemResponse {
	private boolean success;

	public SyncTimeResponse(boolean success) {
		this.success = success;
		this.responseType = ResponseType.synctime;
	}

	public SyncTimeResponse(byte b, String id) {
		if (b == (byte) 0x00) {
			this.success = true;
		} else {
			this.success = false;
		}
		this.setId(id);
	}

	public boolean isSuccess() {
		return success;
	}

	public String toString() {
		if (this.success) {
			return "[" + this.getId() + "] Synctime: success";
		} else {
			return "[" + this.getId() + "] Synctime: failure";
		}
	}
}