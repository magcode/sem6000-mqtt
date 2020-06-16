# MQTT for SEM6000


This tool allows to control VOLTCRAFT SEM6000 BLE (bluetooth low energy) sockets via MQTT.
Based on the great work of [Heckie](https://github.com/Heckie75/voltcraft-sem-6000) and [hypfvieh](https://github.com/hypfvieh/bluez-dbus)

# Supported environments
The tool has been tested on the following environments:

```
Raspberry PI 3B, Bluez, built-in Bluetooth adapter
Virtual machine, Ubuntu 20.04, Bluez, Intel Bluetooth adapter
```


# Configuration
You need a `sem6000.properties` file:

```
rootTopic=home                    # the mqtt root topic
refresh=60                        # number of seconds for MQTT status updates. Do not go below 60!
mqttServer=tcp://192.168.0.1      # IP or hostname of your mqtt broker
logLevel=INFO                     # logLevel
retained=true                     # whether MQTT messages will be sent retained. Defaults to "false"
qos=1                             # the qos which MQTT messages will be sent with. Defaults to "0"

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

Use the following topic and payloads to control the LED:


# Get socket data
The tool will publish the following messages every 60 seconds (as configured in properties file):

```
<roottopic from properties file>/<name of sem6000 from properties file>/
```

# Stability and reconnects
A BLE connection is not stable sometimes. The tools will reconnect to the socket in case the connection gets lost.
