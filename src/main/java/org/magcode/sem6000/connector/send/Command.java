package org.magcode.sem6000.connector.send;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.magcode.sem6000.connector.ByteUtils;

public abstract class Command {
	private byte[] message;
	private byte[] result;
	private boolean processed = false;
	private long sent;
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

	public static byte[] buildMessage(String command, byte[] payload) {
		byte[] bcom = hexStringToByteArray(command);
		byte[] bstart = hexStringToByteArray("0f");
		byte[] bend = hexStringToByteArray("ffff");
		Integer len = 1 + payload.length + bcom.length;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			outputStream.write(bstart);
			outputStream.write(len.byteValue());
			outputStream.write(bcom);
			outputStream.write(payload);
			outputStream.write((byte) 0x00);
			outputStream.write(bend);
		} catch (IOException e) {
			e.printStackTrace();
		}

		byte c[] = outputStream.toByteArray();

		Integer checksum = 1;

		for (int i = 2; i < len + 1; i++) {
			checksum = checksum + c[i];
		}
		c[c.length - 3] = checksum.byteValue();
		return c;
	}

	public static byte[] getMessage(String command, String payload) {
		byte[] bpay = hexStringToByteArray(payload);
		return buildMessage(command, bpay);

	}

	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
    return data;
  }

  public long getSent() {
    return sent;
  }

  public void setSent(long sent) {
    this.sent = sent;
  }

  public String getReadableMessage() {
    return ByteUtils.byteArrayToHex(this.message);
  }
}
