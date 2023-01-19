package org.magcode.sem6000.connector;

import java.nio.ByteBuffer;
import org.magcode.sem6000.connector.receive.ResponseType;
import org.magcode.sem6000.connector.receive.SemResponse;
import org.magcode.sem6000.connector.receive.SemResponseParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Receiver {

  private static final Logger logger = LoggerFactory.getLogger(Receiver.class);
  private String id;
	private byte[] incompleteBuffer;
	private NotificationConsumer notificationReceiver;

	public Receiver(NotificationConsumer notificationReceiver, String id) {
		this.id = id;
		this.notificationReceiver = notificationReceiver;
	}

	public synchronized void receive(byte[] data) {
		logger.debug("[{}] Got notification: {}", this.id, ByteUtils.byteArrayToHex(data));
		byte[] toparse = data;

		// first check if this message can be parsed standalone
		SemResponse resp = SemResponseParser.parseMessage(toparse);
		if (resp.getType() != ResponseType.incomplete) {
			notificationReceiver.receiveSem6000Response(resp);
			this.incompleteBuffer = null;
			return;
		}

		// it's not recognized, so put it to the the buffer
		if (this.incompleteBuffer == null) {
			this.incompleteBuffer = toparse;
			return;
		}

		// it's not recognized and we have some buffer: build buffer+new message
		logger.trace("Found data in buffer: {}", ByteUtils.byteArrayToHex(this.incompleteBuffer));
		ByteBuffer buff = ByteBuffer.allocate(this.incompleteBuffer.length + data.length);
		buff.put(this.incompleteBuffer).put(data);
		// more than 60 bytes are not expected. Thus clearing the buffer.
		if (this.incompleteBuffer != null && this.incompleteBuffer.length > 60) {
			logger.debug("[{}] Clearing buffer", this.id);
			this.incompleteBuffer = null;
			return;
		}
		toparse = buff.array();
		resp = SemResponseParser.parseMessage(toparse);
		if (resp.getType() != ResponseType.incomplete) {
			notificationReceiver.receiveSem6000Response(resp);
			this.incompleteBuffer = null;
		} else {
			this.incompleteBuffer = toparse;
		}
	}

	public synchronized void receive(SemResponse response) {
		notificationReceiver.receiveSem6000Response(response);
	}
}
