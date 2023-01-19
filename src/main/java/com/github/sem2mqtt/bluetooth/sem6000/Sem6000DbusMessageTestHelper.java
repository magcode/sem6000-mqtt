package com.github.sem2mqtt.bluetooth.sem6000;

import static java.util.Collections.emptyList;

import java.util.Map;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;
import org.freedesktop.dbus.types.Variant;
import org.magcode.sem6000.connector.send.Command;

public class Sem6000DbusMessageTestHelper {

  public static PropertiesChanged createMeasurementPropertyChange(String dbusPath) throws DBusException {
    return new PropertiesChanged(dbusPath, "bt_interface1", createMeasurementPropertyChange(), emptyList());
  }

  public static Map<String, Variant<?>> createMeasurementPropertyChange() {
    return Map.of("Value", new Variant<>(Command.hexStringToByteArray("0f11040001002cc8ea0059320000000000006f")));
  }
}
