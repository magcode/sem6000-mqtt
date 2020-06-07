package org.magcode.sem6000.send;

public abstract class Command {
	private byte[] message;
	private byte[] result;
	private boolean processed = false;

	public Command() {

	}

	public Command(byte[] message) {
		setMessage(message);
	}

	public byte[] getMessage() {
		return message;
	}

	public void setMessage(byte[] message) {
		this.message = message;
	}

	public byte[] getResult() {
		return result;
	}

	public void setResult(byte[] result) {
		this.result = result;
	}

	public boolean isProcessed() {
		return processed;
	}

	public void setProcessed(boolean processed) {
		this.processed = processed;
	}
}
