package org.magcode.sem6000;

import tinyb.BluetoothNotification;

public class ConnectedNotification implements BluetoothNotification<Boolean> {

	@Override
	public void run(Boolean arg0) {
		System.out.println(arg0 ? "Connected" : "Disconnected");
	}
}