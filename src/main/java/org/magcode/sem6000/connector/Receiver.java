package org.magcode.sem6000.connector;

import java.nio.ByteBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.magcode.sem6000.connector.receive.ResponseType;
import org.magcode.sem6000.connector.receive.SemResponse;
import org.magcode.sem6000.connector.receive.SemResponseParser;

public class Receiver {
	private static Logger logger = LogManager.getLogger(Receiver.class);
	private String id;
	private byte[] incompleteBuffer;
	private NotificationReceiver notificationReceiver;

	public Receiver(NotificationReceiver notificationReceiver, String id) {
		this.id = id;
		this.notificationReceiver = notificationReceiver;
	}

	public synchronized void receive(byte[] data) {
		logger.debug("[{}] Got notification: {}", this.id, ByteUtils.byteArrayToHex(data));
		byte[] toparse = data;

		if (this.incompleteBuffer != null && this.incompleteBuffer.length > 200) {
			logger.debug("[{}] Clearing buffer", this.id);
			this.incompleteBuffer = null;
		}

		if (this.incompleteBuffer != null) {
			logger.trace("Found data in buffer: {}", ByteUtils.byteArrayToHex(this.incompleteBuffer));
			ByteBuffer buff = ByteBuffer.allocate(this.incompleteBuffer.length + data.length);
			buff.put(this.incompleteBuffer).put(data);
			toparse = buff.array();
		}

		SemResponse resp = SemResponseParser.parseMessage(toparse, this.id);
		if (resp.getType() == ResponseType.incomplete) {
			this.incompleteBuffer = toparse;
			return;
		}
		this.incompleteBuffer = null;
		logger.debug("[{}] Got message with content: {}", this.id, resp.toString());
		notificationReceiver.receiveSem6000Response(resp);

	}
}
