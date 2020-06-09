package org.magcode.sem6000;

import org.magcode.sem6000.connector.Connector;

public class Sem6000Config {
	private String mac;
	private String pin;
	private Connector connector;
	private String name;

	public Sem6000Config(String mac, String pin, String name) {
		this.mac = mac;
		this.pin = pin;
		this.name = name;
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

	public Connector getConnector() {
		return connector;
	}

	public void setConnector(Connector connector) {
		this.connector = connector;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
