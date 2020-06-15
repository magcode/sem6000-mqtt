package org.magcode.sem6000.connector.receive;

public class SwitchResponse extends SemResponse {
	private boolean success;	

	public SwitchResponse(byte b, String id) {
		this.responseType = ResponseType.switchrelay;
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
			return "[" + this.getId() + "] Switch: success";
		} else {
			return "[" + this.getId() + "] Switch: failure";
		}
	}
}