package com.github.sem2mqtt.bluetooth.sem6000;

import static com.coreoz.wisp.schedule.Schedules.executeOnce;
import static com.coreoz.wisp.schedule.Schedules.fixedDelaySchedule;
import static java.util.Collections.emptyMap;

import com.coreoz.wisp.Scheduler;
import com.coreoz.wisp.schedule.Schedules;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattService;
import com.github.sem2mqtt.bluetooth.BluetoothConnection;
import com.github.sem2mqtt.bluetooth.BluetoothConnectionManager;
import com.github.sem2mqtt.bluetooth.sem6000.Sem6000DbusHandlerProxy.Sem6000ResponseHandler;
import com.github.sem2mqtt.configuration.Sem6000Config;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezInvalidValueLengthException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotPermittedException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.magcode.sem6000.connector.receive.AvailabilityResponse;
import org.magcode.sem6000.connector.receive.AvailabilityResponse.Availability;
import org.magcode.sem6000.connector.receive.SemResponse;
import org.magcode.sem6000.connector.send.Command;
import org.magcode.sem6000.connector.send.DataDayCommand;
import org.magcode.sem6000.connector.send.LoginCommand;
import org.magcode.sem6000.connector.send.MeasureCommand;
import org.magcode.sem6000.connector.send.SyncTimeCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sem6000Connection extends BluetoothConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(Sem6000Connection.class);
  private static final Duration RECONNECT_DELAY = Duration.ofSeconds(60);
  private final Sem6000Config sem6000Config;
  private BluetoothDevice device;
  private BluetoothGattCharacteristic writeService;
  private BluetoothGattCharacteristic notifyService;
  /* Name of schedule if reconnection is scheduled, null otherwise. */
  private String reconnectScheduleName;
  /* Name of schedule for measurements if connected, null otherwise. */
  private String measurementSchedulerName;
  private final Set<Sem6000ResponseHandler> subscribers = new CopyOnWriteArraySet<>();

  public Sem6000Connection(Sem6000Config sem6000Config, BluetoothConnectionManager connectionManager,
      Scheduler scheduler) {
    super(connectionManager, scheduler);
    this.sem6000Config = sem6000Config;
  }


  @Override
  public String getMacAddress() {
    return sem6000Config.getMac();
  }

  public void establish() {
    LOGGER.info("Establishing connection to {} ({})", sem6000Config.getName(), sem6000Config.getMac());
    try {
      connectToDevice();
    } catch (ConnectException e) {
      LOGGER.warn("Device {} is not connected properly. Scheduling reconnect. ", this.sem6000Config.getName());
      scheduleReconnect();
    } catch (RuntimeException e) {
      LOGGER.warn("General exception, device {} is not connected properly. Scheduling reconnect. ",
          this.sem6000Config.getName());
      scheduleReconnect();
    }
  }

  private void connectToDevice() throws ConnectException {
    device = connectionManager.findDeviceOrFail(sem6000Config.getMac(), new ConnectException("Could not find device."));
    if (!device.connect()) {
      throw new ConnectException("Could not connect to device.");
    }

    loadGattCharacteristicsAndSubscribeChanges();

    try {
      this.safeSend(new LoginCommand(this.sem6000Config.getPin()));
      // add some delay so that device can handle login properly
      Thread.sleep(200);
      this.safeSend(new SyncTimeCommand());
      handleConnected();
    } catch (SendingException | InterruptedException e) {
      LOGGER.debug("Could not connect device with error: ", e);
      throw new ConnectException(e);
    }
  }

  private void loadGattCharacteristicsAndSubscribeChanges() throws ConnectException {
    BluetoothGattService gattService = device.getGattServiceByUuid(Sem6000GattCharacteristic.Service.uuid);
    writeService = gattService.getGattCharacteristicByUuid(Sem6000GattCharacteristic.Write.uuid);
    notifyService = gattService.getGattCharacteristicByUuid(
        Sem6000GattCharacteristic.Notify.uuid);
    try {
      notifyService.startNotify();
      connectionManager.subscribeToDbusPath(notifyService.getDbusPath(),
          new Sem6000DbusHandlerProxy(this::handleResponse));
    } catch (BluezFailedException | BluezInProgressException | BluezNotSupportedException |
             BluezNotPermittedException e) {
      LOGGER.debug("Could not connect device, because starting notify failed: ", e);
      throw new ConnectException(e);
    }
  }

  protected void handleConnected() {
    LOGGER.info("Successfully connected to device {} ('{}')", sem6000Config.getName(), sem6000Config.getMac());
    reconnectScheduleName = null;
    subscribers.forEach(handler -> handler.handleSem6000Response(new AvailabilityResponse(Availability.available)));
    measurementSchedulerName = scheduler.schedule(this::requestMeasurements,
        Schedules.fixedDelaySchedule(this.sem6000Config.getUpdateInterval())).name();
  }

  private void requestMeasurements() {
    try {
      safeSend(new MeasureCommand());
      safeSend(new DataDayCommand());
    } catch (SendingException e) {
      LOGGER.warn("Could not request measurement from device {}: {}", sem6000Config.getName(), e.getMessage());
      LOGGER.debug("Requesting measurement errored with: ", e);
    }
  }

  private void handleResponse(SemResponse semResponse) {
    subscribers.forEach(handler -> handler.handleSem6000Response(semResponse));
    subscribers.forEach(handler -> handler.handleSem6000Response(new AvailabilityResponse(Availability.available)));
  }

  private void handleDisconnected() {
    LOGGER.info("Lost connection to device {} ('{}')", sem6000Config.getName(), sem6000Config.getMac());
    scheduler.cancel(measurementSchedulerName);
    measurementSchedulerName = null;
    subscribers.forEach(handler -> handler.handleSem6000Response(new AvailabilityResponse(Availability.lost)));
    connectionManager.ignoreDbusPath(notifyService.getDbusPath());
    device = null;
    writeService = null;
    notifyService = null;
    scheduleReconnect();
  }

  private void reconnect(int attempt) {
    LOGGER.debug("Trying to connect to device {} in attempt {}.", this.sem6000Config.getName(), attempt);
    try {
      connectToDevice();
    } catch (ConnectException e) {
      LOGGER.warn("Failed to connect to device {} in attempt {}. Rescheduling reconnect.", sem6000Config.getName(),
          attempt);
      reconnectScheduleName = scheduler.schedule(() -> reconnect(attempt + 1),
          executeOnce(fixedDelaySchedule(RECONNECT_DELAY))).name();
    }
  }

  public synchronized void safeSend(Command command) throws SendingException {
    ensureConnectionIsEstablishedOrThrow(new SendingException(
        String.format("Failed to send message because device %s is not connected.", this.sem6000Config.getName())));
    LOGGER.debug("Sending command to {} ('{}')", this.sem6000Config.getName(), command.getReadableMessage());
    try {
      writeService.writeValue(command.getMessage(), emptyMap());
    } catch (BluezFailedException | BluezNotAuthorizedException | BluezInvalidValueLengthException |
             BluezNotSupportedException | BluezInProgressException | BluezNotPermittedException e) {
      throw new SendingException(String.format("Failed to send bluetooth message to %s", this.sem6000Config.getName()),
          e);
    }
  }

  private <T extends Throwable> void ensureConnectionIsEstablishedOrThrow(T exception) throws T {
    if (!isEstablished()) {
      handleDisconnected();
      throw exception;
    }
  }

  public boolean isEstablished() {
    return Objects.nonNull(this.device) && Objects.nonNull(this.writeService) && Objects.nonNull(this.notifyService)
        && this.device.isConnected();
  }

  private void scheduleReconnect() {
    if (Objects.isNull(reconnectScheduleName)) {
      LOGGER.debug("Scheduling reconnect for device {}.", sem6000Config.getName());
      reconnectScheduleName = scheduler.schedule(() -> reconnect(0),
              executeOnce(fixedDelaySchedule(RECONNECT_DELAY)))
          .name();
    }
  }

  public void subscribe(Sem6000ResponseHandler responseHandler) {
    subscribers.add(responseHandler);
  }
}
