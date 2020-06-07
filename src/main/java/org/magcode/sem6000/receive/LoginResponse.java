package org.magcode.sem6000.receive;

public class LoginResponse extends SemResponse {
	private boolean success;

	public LoginResponse(boolean success) {
		this.success = success;
		this.responseType = ResponseType.login;
	}

	public boolean isSuccess() {
		return success;
	}

	public String toString() {
		return this.success ? "Login: success" : "Login: failure";
	}
}