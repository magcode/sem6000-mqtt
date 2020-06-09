package org.magcode.sem6000.connector;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.magcode.sem6000.connector.receive.ResponseType;
import org.magcode.sem6000.connector.receive.SemResponse;
import org.magcode.sem6000.connector.receive.SemResponseParser;
import org.magcode.sem6000.connector.send.Command;

import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothNotification;

public class SendReceiveThread implements Runnable, BluetoothNotification<byte[]> {
	private final BlockingQueue<Command> workQueue;
	private Command currentMessage;
	BluetoothGattCharacteristic writeChar;
	private byte[] incompleteBuffer;
	private static Logger logger = LogManager.getLogger(SendReceiveThread.class);
	private NotificationReceiver receiver;
	private String id = "";

	public SendReceiveThread(BlockingQueue<Command> workQueue, BluetoothGattCharacteristic writeChar,
			NotificationReceiver receiver, String id) {
		logger.trace("Thread started");
		this.workQueue = workQueue;
		this.writeChar = writeChar;
		this.receiver = receiver;
		this.id = id;
	}

	public String toString() {
		return this.id;
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				boolean take = true;
				if (workQueue.isEmpty()) {
					take = false;
				}

				if (this.currentMessage != null && !this.currentMessage.isProcessed()) {
					take = false;
				}
				if (take) {
					this.currentMessage = workQueue.take();
					logger.trace("Took command from sending queue: {}. Items left: {}",
							ByteUtils.byteArrayToHex(this.currentMessage.getMessage()), workQueue.size());
					this.writeChar.writeValue(this.currentMessage.getMessage());
				}
				Thread.sleep(200);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		logger.trace("Terminated");
	}

	@Override
	public synchronized void run(byte[] arg0) {
		logger.trace("Got notification: {}", ByteUtils.byteArrayToHex(arg0));
		byte[] toparse = arg0;
		if (this.incompleteBuffer != null) {
			logger.trace("Found data in buffer: {}", ByteUtils.byteArrayToHex(this.incompleteBuffer));
			ByteBuffer buff = ByteBuffer.allocate(this.incompleteBuffer.length + arg0.length);
			buff.put(this.incompleteBuffer).put(arg0);
			toparse = buff.array();
		}

		SemResponse resp = SemResponseParser.parseMessage(toparse, this.id);
		if (resp.getType() == ResponseType.incomplete) {
			this.incompleteBuffer = toparse;
			return;
		}
		this.incompleteBuffer = null;
		logger.debug("[{}] Got message with content: {}", this.id, resp.toString());
		if (this.currentMessage != null && !this.currentMessage.isProcessed()) {
			this.currentMessage.setResult(arg0);
			this.currentMessage.setProcessed(true);
			logger.trace("Processed command: {}", ByteUtils.byteArrayToHex(this.currentMessage.getMessage()));
			if (receiver != null) {
				receiver.receiveSem6000Response(resp);
			}
		} else {

		}
	}
}
