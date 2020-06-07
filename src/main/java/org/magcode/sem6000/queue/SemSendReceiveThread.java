package org.magcode.sem6000.queue;

import java.util.concurrent.BlockingQueue;

import org.magcode.sem6000.Sem6BleApp;
import org.magcode.sem6000.receive.SemResponse;
import org.magcode.sem6000.receive.SemResponseParser;
import org.magcode.sem6000.send.Command;

import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothNotification;

public class SemSendReceiveThread implements Runnable, BluetoothNotification<byte[]> {
	private final BlockingQueue<Command> workQueue;
	private Command currentMessage;
	BluetoothGattCharacteristic writeChar;

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
					System.out.println("TOOK: "
							+ Sem6BleApp.byteArrayToHex(this.currentMessage.getMessage()) + " left: "
							+ workQueue.size());
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
	public void run(byte[] arg0) {
		System.out.println("Got notification: " + Sem6BleApp.byteArrayToHex(arg0));
		SemResponse resp = SemResponseParser.parseMessage(arg0);
		System.out.println("Got response of type:" + resp.getType() + " " + resp.toString());
		if (this.currentMessage != null && !this.currentMessage.isProcessed()) {
			this.currentMessage.setResult(arg0);
			this.currentMessage.setProcessed(true);
			System.out.println("Processed: " + Sem6BleApp.byteArrayToHex(this.currentMessage.getMessage()));
		} else {

		}
	}
}
