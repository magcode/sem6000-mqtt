package org.magcode.sem6000.connector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.handlers.AbstractPropertiesChangedHandler;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;
import org.freedesktop.dbus.types.Variant;
import org.magcode.sem6000.connector.send.Command;
import org.magcode.sem6000.mqtt.Sem6000Config;

import com.github.hypfvieh.bluetooth.DeviceManager;

public class ConnectionManager extends AbstractPropertiesChangedHandler {
	private static Logger logger = LogManager.getLogger(ConnectionManager.class);
	private DeviceManager manager;
	private Map<String, Connector> sems = new HashMap<String, Connector>();
	private Map<String, Receiver> gattDataReceivers = new HashMap<String, Receiver>();
	private NotificationConsumer receiver;
	private int consecutiveReconnectLimit;

	public ConnectionManager(NotificationConsumer receiver, int consecutiveReconnectLimit) {
		this.receiver = receiver;
		this.consecutiveReconnectLimit = consecutiveReconnectLimit;
	}

	public void init() {
		try {
			this.manager = DeviceManager.createInstance(false);
			this.manager.scanForBluetoothDevices(10 * 1000);
			this.manager.registerPropertyHandler(this);
			//manager.registerSignalHandler(new MySignalHandler());
		} catch (DBusException e) {
			logger.error("DBusException.", e);
		}
	}

	public void addSem(Sem6000Config config) {
		Receiver gattDataReceiver = new Receiver(this.receiver, config.getName());
		gattDataReceivers.put(config.getName(), gattDataReceiver);
		Connector connector = new Connector(this.manager, config.getMac(), config.getPin(), config.getName(),
				config.getUpdateSeconds(), this.consecutiveReconnectLimit, gattDataReceiver);
		sems.put(config.getName(), connector);
	}

	public void sendCommand(String id, Command command) {
		Connector connector = this.sems.get(id);
		if (connector == null || command == null) {
			logger.warn("Unknown id {} or command empty", id);
		} else {
			connector.send(command);
		}
	}

	public void shutDown() {
		logger.debug("Shut down started");
		Iterator<String> it = sems.keySet().iterator();
		while (it.hasNext()) {
			String id = it.next();
			Connector connector = sems.get(id);
			connector.stop();
		}
		logger.debug("closing connection");
		this.manager.closeConnection();
	}

	@Override
	public void handle(PropertiesChanged props) {
		if (props != null) {
			Iterator<String> it = gattDataReceivers.keySet().iterator();
			while (it.hasNext()) {
				String id = it.next();
				Connector connector = sems.get(id);
				if (connector !=null && props.getPath().equals(connector.getNotifyCharPath())) {
					Map<String, Variant<?>> data = props.getPropertiesChanged();
					if (data.containsKey("Value")) {
						Variant<?> payload = data.get("Value");
						Object valO = payload.getValue();
						if (valO instanceof byte[]) {
							byte[] xx = (byte[]) valO;
							Receiver gattDataReceiver = gattDataReceivers.get(id);
							gattDataReceiver.receive(xx);

						} else {
							System.err.println("is a " + valO.getClass().toString());
						}
					}
				}
			}
		}
	}
}