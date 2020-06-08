package org.magcode.sem6000.queue;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.magcode.sem6000.Sem6000MQTT;
import org.magcode.sem6000.receive.ResponseType;
import org.magcode.sem6000.receive.SemResponse;
import org.magcode.sem6000.receive.SemResponseParser;
import org.magcode.sem6000.send.Command;

import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothNotification;

public class SemSendReceiveThread implements Runnable, BluetoothNotification<byte[]> {
	private final BlockingQueue<Command> workQueue;
	private Command currentMessage;
	BluetoothGattCharacteristic writeChar;
	private byte[] incompleteBuffer;
	private static Logger logger = LogManager.getLogger(SemSendReceiveThread.class);

	public SemSendReceiveThread(BlockingQueue<Command> workQueue2, BluetoothGattCharacteristic writeChar) {
		this.workQueue = workQueue2;
		this.writeChar = writeChar;
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
					logger.debug("Took command from sending queue: {}. Items left: {}",
							Sem6000MQTT.byteArrayToHex(this.currentMessage.getMessage()), workQueue.size());
					this.writeChar.writeValue(this.currentMessage.getMessage());
				}
				Thread.sleep(200);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	@Override
	public synchronized void run(byte[] arg0) {
		logger.debug("Got notification: {}", Sem6000MQTT.byteArrayToHex(arg0));
		byte[] toparse = arg0;
		if (this.incompleteBuffer != null) {
			logger.trace("Found data in buffer: {}", Sem6000MQTT.byteArrayToHex(this.incompleteBuffer));
			ByteBuffer buff = ByteBuffer.allocate(this.incompleteBuffer.length + arg0.length);
			buff.put(this.incompleteBuffer).put(arg0);
			toparse = buff.array();
		}

		SemResponse resp = SemResponseParser.parseMessage(toparse);
		if (resp.getType() == ResponseType.incomplete) {
			this.incompleteBuffer = toparse;
			return;
		}
		this.incompleteBuffer = null;
		logger.debug("Got message with content: {}", resp.toString());
		if (this.currentMessage != null && !this.currentMessage.isProcessed()) {
			this.currentMessage.setResult(arg0);
			this.currentMessage.setProcessed(true);
			logger.debug("Processed command: {}", Sem6000MQTT.byteArrayToHex(this.currentMessage.getMessage()));
		} else {

		}
	}
}
