package org.magcode.sem6000;

import org.magcode.sem6000.receive.SemResponse;

public interface NotificatioReceiver {
	public void receiveSem6000Response(SemResponse response);
}
