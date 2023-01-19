package com.github.sem2mqtt.bluetooth.sem6000;

import com.github.sem2mqtt.bluetooth.DevicePropertiesChangedHandler.DbusListener;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;
import org.freedesktop.dbus.types.Variant;
import org.magcode.sem6000.connector.ByteUtils;
import org.magcode.sem6000.connector.receive.ResponseType;
import org.magcode.sem6000.connector.receive.SemResponse;
import org.magcode.sem6000.connector.receive.SemResponseParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responses of the sem6000 device may be split into multiple parts. The proxy transparently handles this issue and
 * responds with a single Sem6000Response.
 */
public class Sem6000DbusHandlerProxy implements DbusListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(Sem6000DbusHandlerProxy.class);
  private static final int MAX_EXPECTED_RESPONSE_LENGTH = 60;
  private final Sem6000ResponseHandler responseHandler;
  private byte[] buffer;

  public Sem6000DbusHandlerProxy(Sem6000ResponseHandler responseHandler) {

    this.responseHandler = responseHandler;
  }

  @Override
  public void handle(PropertiesChanged propertiesChanged) {
    LOGGER.debug("Received message for path '{}'.", propertiesChanged.getPath());
    Optional.ofNullable(propertiesChanged.getPropertiesChanged().get("Value")).map(Variant::getValue).filter(
        byte[].class::isInstance).map(byte[].class::cast).ifPresent(data -> {
      SemResponse responseFromData = SemResponseParser.parseMessage(data);
      boolean isResponseComplete = responseFromData.getType() != ResponseType.incomplete;
      if (isResponseComplete) {
        LOGGER.debug("Received message for dbus path '{}': '{}'.", propertiesChanged.getPath(),
            ByteUtils.byteArrayToHex(data));
        responseHandler.handleSem6000Response(responseFromData);
        buffer = null;
      } else if (Objects.nonNull(buffer)) {
        handleIncompleteConsecutiveResponse(data, propertiesChanged.getPath());
      } else {
        LOGGER.debug("Part of sem 6000 response for path '{}' buffered.", propertiesChanged.getPath());
        buffer = data;
      }
    });
  }

  private void handleIncompleteConsecutiveResponse(byte[] data, String path) {
    byte[] combinedData = Arrays.copyOf(buffer, buffer.length + data.length);
    if (combinedData.length > MAX_EXPECTED_RESPONSE_LENGTH) {
      LOGGER.debug("Previous response for dbus path '{}' is dismissed, because a new response arrived.", path);
      buffer = data;
    } else {
      System.arraycopy(data, 0, combinedData, buffer.length, data.length);
      SemResponse response = SemResponseParser.parseMessage(data);
      if (response.getType() != ResponseType.incomplete) {
        LOGGER.debug("Received last part of message for  dbus path '{}': '{}'.", path,
            ByteUtils.byteArrayToHex(combinedData));
        responseHandler.handleSem6000Response(response);
        buffer = null;
      } else {
        LOGGER.debug("Part of sem 6000 response for dbus path '{}' is buffered.", path);
        buffer = combinedData;
      }
    }
  }

  public interface Sem6000ResponseHandler {

    void handleSem6000Response(SemResponse semResponse);
  }
}
