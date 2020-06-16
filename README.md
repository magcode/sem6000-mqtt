# MQTT for SEM6000

This tool allows to control VOLTCRAFT SEM6000 BLE (bluetooth low energy) sockets via MQTT and therefore easy integration in the home automation of your choice!
Based on the great work of [Heckie](https://github.com/Heckie75/voltcraft-sem-6000) and [hypfvieh](https://github.com/hypfvieh/bluez-dbus).
At the moment only crucial features are implemented: switching the relay and getting power and today's consumption values.

# Supported environments
The tool has been tested on the following environments:

```
Raspberry PI 3B, Bluez, built-in Bluetooth adapter
Virtual machine, Ubuntu 20.04, Bluez, Intel Bluetooth adapter
```
# Please contribute

For problems: Report issues.
For the missing features: Create pull requests.

# Configuration
You need a `sem6000.properties` file:

```
rootTopic=home                    # the mqtt root topic
refresh=60                        # number of seconds for MQTT status updates. Do not go below 60!
mqttServer=tcp://192.168.0.1      # IP or hostname of your mqtt broker
logLevel=INFO                     # log level

daikin1.host=192.168.0.2          # IP adress of your first Daikin Wifi adapter
daikin1.name=ac-room1             # a name for the Daikin device, used in the MQTT topic

daikin2.host=192.168.0.3
daikin2.name=ac-room2
```

# Running
It can be simply run with

`java -jar /var/javaapps/daikin/daikin-mqtt-1.1.0-jar-with-dependencies.jar`

Don't forget to put the `daikin.properties` right beside the jar file.


# Control sockets
Use the following topic and payloads to control the relay:
```
<roottopic from properties file>/<name of sem6000 from properties file>/relay/set (true|false)
```
Use the following topic and payloads to control the LED:

```
<roottopic from properties file>/<name of sem6000 from properties file>/led/set (true|false)
```

# Get socket data
The tool will publish the following messages every 60 seconds (as configured in properties file):

```
<roottopic from properties file>/<name of sem6000 from properties file>/
```

# Stability and reconnects
A BLE connection is not stable sometimes. The tools will attempt to reconnect to the socket after five minutes in case the connection gets lost.
