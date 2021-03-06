package org.magcode.sem6000.mqtt;

public class Sem6000Config {
	private String mac;
	private String pin;
	private String name;
	private int updateSeconds;

	public Sem6000Config() {
	}

	public Sem6000Config(String mac, String pin, String name, int updateSeconds) {
		this.mac = mac;
		this.pin = pin;
		this.name = name;
		this.updateSeconds = updateSeconds;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}

	public String getPin() {
		return pin;
	}

	public void setPin(String pin) {
		this.pin = pin;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getUpdateSeconds() {
		return updateSeconds;
	}

	public void setUpdateSeconds(int updateSeconds) {
		this.updateSeconds = updateSeconds;
	}
}
