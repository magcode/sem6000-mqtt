package org.magcode.sem6000.connectorv3;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.handlers.AbstractPropertiesChangedHandler;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;
import org.freedesktop.dbus.types.Variant;
import org.magcode.sem6000.connector.ByteUtils;
import org.magcode.sem6000.connector.NotificationReceiver;
import org.magcode.sem6000.connector.receive.ResponseType;
import org.magcode.sem6000.connector.receive.SemResponse;
import org.magcode.sem6000.connector.receive.SemResponseParser;
import org.magcode.sem6000.connector.send.Command;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;

public class SendReceiver implements Runnable {
	private final BlockingQueue<Command> workQueue;
	private Command currentMessage;

	private BluetoothGattCharacteristic writeChar;
	private byte[] incompleteBuffer;
	private static Logger logger = LogManager.getLogger(SendReceiver.class);
	private NotificationReceiver receiver;
	private String id = "";

	public SendReceiver(BlockingQueue<Command> workQueue, BluetoothGattCharacteristic writeChar,
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
					long curTime = System.currentTimeMillis();
					if (curTime - this.currentMessage.getSent() > 4000) {
						logger.info("Took too long.");
					} else {
						take = false;
					}
				}

				if (take) {
					this.currentMessage = workQueue.take();
					logger.trace("Took command from sending queue: {}. Items left: {}",
							ByteUtils.byteArrayToHex(this.currentMessage.getMessage()), workQueue.size());
					this.writeChar.writeValue(this.currentMessage.getMessage(), null);
					this.currentMessage.setSent(System.currentTimeMillis());
				}
				Thread.sleep(200);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				break;
			} catch (DBusException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		logger.trace("Terminated");
	}

	private synchronized void handleData(byte[] data) {
		logger.debug("[{}] Got notification: {}", this.id, ByteUtils.byteArrayToHex(data));
		byte[] toparse = data;

		if (this.incompleteBuffer != null && this.incompleteBuffer.length > 500) {
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
		if (this.currentMessage != null && !this.currentMessage.isProcessed()) {
			this.currentMessage.setResult(data);
			this.currentMessage.setProcessed(true);
			logger.trace("Processed command: {}", ByteUtils.byteArrayToHex(this.currentMessage.getMessage()));
			if (receiver != null) {
				receiver.receiveSem6000Response(resp);
			}
		} else {

		}
	}

}
