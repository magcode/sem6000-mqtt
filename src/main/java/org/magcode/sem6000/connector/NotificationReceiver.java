package org.magcode.sem6000.connector;

import org.magcode.sem6000.connector.receive.SemResponse;

public interface NotificationReceiver {
	public void receiveSem6000Response(SemResponse response);
}
