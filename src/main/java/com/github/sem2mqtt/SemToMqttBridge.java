package com.github.sem2mqtt;

import static java.time.ZonedDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.coreoz.wisp.Scheduler;
import com.github.sem2mqtt.bluetooth.BluetoothConnectionManager;
import com.github.sem2mqtt.bluetooth.sem6000.Sem6000Connection;
import com.github.sem2mqtt.configuration.Sem6000Config;
import com.github.sem2mqtt.mqtt.MqttConnection;
import com.github.sem2mqtt.mqtt.MqttConnection.MessageCallback;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Set;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.magcode.sem6000.connector.receive.AvailabilityResponse;
import org.magcode.sem6000.connector.receive.AvailabilityResponse.Availability;
import org.magcode.sem6000.connector.receive.DataDayResponse;
import org.magcode.sem6000.connector.receive.MeasurementResponse;
import org.magcode.sem6000.connector.receive.SemResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemToMqttBridge {

  private static final Logger LOGGER = LoggerFactory.getLogger(SemToMqttBridge.class);

  private final MqttConnection mqttConnection;
  private final Set<Sem6000Config> sem6000Configs;
  private final String rootTopic;
  private final BluetoothConnectionManager bluetoothConnectionManager;
  private final Scheduler scheduler;
  private ZonedDateTime lastNotifiedAboutOnlineAvailabilityAt;

  public SemToMqttBridge(String rootTopic, Set<Sem6000Config> sem6000Configs, MqttConnection mqttConnection,
      BluetoothConnectionManager bluetoothConnectionManager, Scheduler scheduler) {

    this.mqttConnection = mqttConnection;
    this.sem6000Configs = sem6000Configs;
    this.rootTopic = rootTopic;
    this.bluetoothConnectionManager = bluetoothConnectionManager;
    this.scheduler = scheduler;
  }

  public void run() {
    LOGGER.info("Starting bridge service.");
    mqttConnection.establish();
    bluetoothConnectionManager.init();
    sem6000Configs.stream().peek(this::subscribeToSem6000MqttTopics).forEach(sem6000Config -> {
      Sem6000Connection sem6000Connection = bluetoothConnectionManager.setupConnection(
          new Sem6000Connection(sem6000Config, bluetoothConnectionManager, scheduler));
      sem6000Connection.establish();
      sem6000Connection.subscribe(semResponse -> this.handleSem6000Response(semResponse, sem6000Config));
    });
  }


  void subscribeToSem6000MqttTopics(Sem6000Config sem6000Config) {
    mqttConnection.subscribe(String.format("%s/%s/+/set", rootTopic, sem6000Config.getName()),
        createMessageCallbackFor(sem6000Config));
  }

  private MessageCallback createMessageCallbackFor(Sem6000Config sem6000Config) {
    return (String topic, MqttMessage message) -> handleMqttMessage(topic, message, sem6000Config);
  }

  void handleMqttMessage(String topic, MqttMessage message, Sem6000Config sem6000Config) {

  }

  private synchronized void handleSem6000Response(SemResponse response, Sem6000Config sem6000Config) {
    switch (response.getType()) {
      case measure:
        MeasurementResponse mr = (MeasurementResponse) response;
        LOGGER.info("Forwarding sem6000 measurement '{}' to mqtt for device '{}'", mr, sem6000Config.getName());
        mqttConnection.publish(rootTopic + "/" + sem6000Config.getName() + "/voltage", mr.getVoltage());
        mqttConnection.publish(rootTopic + "/" + sem6000Config.getName() + "/power", mr.getPower());
        mqttConnection.publish(rootTopic + "/" + sem6000Config.getName() + "/relay", mr.isPowerOn());
        break;
      case dataday:
        DataDayResponse dr = (DataDayResponse) response;
        LOGGER.info("Forwarding daily data response '{}' to mqtt for device '{}'", dr, sem6000Config.getName());
        mqttConnection.publish(rootTopic + "/" + sem6000Config.getName() + "/energytoday", dr.getToday());
        break;
      case availability:
        AvailabilityResponse ar = (AvailabilityResponse) response;
        // avoid notifying about online state too often
        if (ar.getAvailability() != Availability.available || Objects.isNull(lastNotifiedAboutOnlineAvailabilityAt)
            || now().isAfter(
            lastNotifiedAboutOnlineAvailabilityAt.plus(sem6000Config.getUpdateInterval().toSeconds(), SECONDS))) {
          LOGGER.info("Forwarding sem6000 availability '{}' to mqtt for device '{}'", ar, sem6000Config.getName());
          String payload = ar.getAvailability() == Availability.available ? "online" : "lost";
          mqttConnection.publish(rootTopic + "/" + sem6000Config.getName() + "/state", payload);
          // when notifying about online state, update last notification time
          lastNotifiedAboutOnlineAvailabilityAt =
              ar.getAvailability() == Availability.available ? now() : lastNotifiedAboutOnlineAvailabilityAt;
        }
        break;
      default:
        break;
    }
  }
}
