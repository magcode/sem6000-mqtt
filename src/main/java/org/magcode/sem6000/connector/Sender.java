package org.magcode.sem6000.connector;

import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freedesktop.dbus.exceptions.DBusException;
import org.magcode.sem6000.connector.send.Command;

import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;

public class Sender implements Runnable {
	private final BlockingQueue<Command> workQueue;
	private BluetoothGattCharacteristic writeChar;
	private static Logger logger = LogManager.getLogger(Sender.class);
	private String id = "";

	public Sender(BlockingQueue<Command> workQueue, BluetoothGattCharacteristic writeChar,
			NotificationConsumer receiver, String id) {
		logger.trace("Thread started");
		this.workQueue = workQueue;
		this.writeChar = writeChar;
		this.id = id;
	}

	public String toString() {
		return this.id;
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				Command currentCommand = workQueue.take();
				logger.trace("Took command from sending queue: {}. Items left: {}",
						ByteUtils.byteArrayToHex(currentCommand.getMessage()), workQueue.size());
				this.writeChar.writeValue(currentCommand.getMessage(), null);
				Thread.sleep(1000);
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
}
