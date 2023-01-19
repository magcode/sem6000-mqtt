package com.github.sem2mqtt.bluetooth;

import static java.util.Collections.emptySet;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.freedesktop.dbus.handlers.AbstractPropertiesChangedHandler;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DevicePropertiesChangedHandler extends AbstractPropertiesChangedHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DevicePropertiesChangedHandler.class);

  private final Map<String, Set<DbusListener>> dbusListenerMap = new ConcurrentHashMap<>();

  void subscribe(String dbusPath, DbusListener listener) {
    LOGGER.debug("Subscribed listener for dbus path '{}'.", dbusPath);
    dbusListenerMap.putIfAbsent(dbusPath, new CopyOnWriteArraySet<>());
    dbusListenerMap.get(dbusPath).add(listener);
  }

  @Override
  public void handle(PropertiesChanged propertiesChanged) {
    LOGGER.debug("Received properties changed ({}) for path '{}'.", propertiesChanged.toString(),
        propertiesChanged.getPath());
    dbusListenerMap.getOrDefault(propertiesChanged.getPath(), emptySet())
        .forEach(dbusListener -> dbusListener.handle(propertiesChanged));
  }

  public void ignore(String dbusPath) {
    dbusListenerMap.remove(dbusPath);
  }

  public interface DbusListener {

    void handle(PropertiesChanged changedProperties);
  }
}
