package org.magcode.sem6000.connector.receive;

public class LoginResponse extends SemResponse {
	private boolean success;

	public LoginResponse(byte b, String id) {
		if (b == (byte) 0x00) {
			this.success = true;
		} else {
			this.success = false;
		}
		this.setId(id);
		this.responseType = ResponseType.login;
	}

	public boolean isSuccess() {
		return success;
	}

	public String toString() {
		if (this.success) {
			return "[" + this.getId() + "] Login: success";
		} else {
			return "[" + this.getId() + "] Login: failure";
		}
	}
}