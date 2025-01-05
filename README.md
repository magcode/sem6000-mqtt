# MQTT for VOLTCRAFT SEM6000

This tool allows to control VOLTCRAFT SEM6000 BLE (bluetooth low energy) power measurement sockets via MQTT and therefore easy integration in the home automation of your choice!

Based on the great work of [Heckie](https://github.com/Heckie75/voltcraft-sem-6000) and [hypfvieh](https://github.com/hypfvieh/bluez-dbus).

At the moment only crucial features are implemented: switching the relay, getting power and today's consumption values.

# Supported environments
The tool only runs on Linux and requires Java and [Bluez](http://www.bluez.org/).
It has been tested on the following environments:

```
Raspberry PI 3 B, Raspbian 10 Buster, OpenJDK 11.0.7, Bluez 5.50, built-in Bluetooth adapter usb:v1D6Bp0246d0532
Raspberry PI 3 B, Raspbian 10 Buster, OpenJDK 11.0.7, Bluez 5.50, USB Bluetooth adapter 0a12:0001 Cambridge Silicon Radio, Ltd Bluetooth Dongle
Virtual machine, Ubuntu 20.04, OpenJDK 11.0.7, Bluez 5.54, Intel Bluetooth adapter 8087:0a2b
```
# Please contribute

For problems: Report [issues](https://github.com/magcode/sem6000-mqtt/issues).

For the missing features: Create pull requests.

# Configuration
You need a `sem6.properties` file where you can configure multiple SEM6000 devices.

```
rootTopic=home/mysemdevices             # the mqtt root topic
mqttServer=tcp://192.168.0.1            # IP or hostname of your mqtt broker
username=mqttuser                       # username for mqtt broker (optional, leave empty if not needed)
password=mqttpassword                   # password for mqtt broker (optional, leave empty if not needed)
mqttDiscovery=true                      # set to false if you don't want to use mqtt discovery
homeassistantRootTopic=homeassistant    # the root topic for homeassistant discovery messages

sem1.mac=00:00:00:00:00:01              # the mac of your sem6000 device
sem1.pin=0000                           # the PIN of your sem6000 device
sem1.name=sem1                          # the name of your sem6000 device, use [a-z0-9]
sem1.refresh=60                         # the schedule to send MQTT status information, seconds. Do not go below 30.

sem2.mac=00:00:00:00:00:02
sem2.pin=0000
sem2.name=sem2
sem2.refresh=60
```

How to get the mac? Log on to your host and enter
```
bluetoothctl
devices
```

# Running
It can simply be run with

`java -jar sem6000-mqtt-1.0.0-jar-with-dependencies.jar`

Don't forget to put the `sem6.properties` right beside the jar file.

## MQTT Discovery
If you set `mqttDiscovery=true` in the properties file, the tool will expose the SEM6000 devices to Home Assistant via MQTT discovery. The devices will be automatically discovered and added to Home Assistant.


# Control sockets
Use the following topic and payloads to control the relay:
```
<roottopic from properties file>/<name of sem6000 from properties file>/relay/set (true|false)
```
Use the following topic and payloads to enable/disable the LED:

```
<roottopic from properties file>/<name of sem6000 from properties file>/led/set (true|false)
```

# Get socket data
The tool will publish the following messages every 60 seconds (as configured in properties file):

```
<roottopic from properties file>/<name of sem6000 from properties file>/voltage     (voltage)
<roottopic from properties file>/<name of sem6000 from properties file>/power       (current power in watts)
<roottopic from properties file>/<name of sem6000 from properties file>/relay       (relay, true or false)
<roottopic from properties file>/<name of sem6000 from properties file>/energytoday (consumed energy since midnight in watt hours)
```

# Stability and reconnects
A BLE connection is not stable sometimes. The tool will attempt to reconnect to the socket after five minutes in case the connection gets lost. It will stop reconnecting after reaching 100 consecutive unsuccessful reconnects.
