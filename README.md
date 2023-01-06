# MQTT for VOLTCRAFT SEM6000 [![Java CI with Maven](https://github.com/steineggerroland/sem6000-mqtt/actions/workflows/maven.yml/badge.svg)](https://github.com/steineggerroland/sem6000-mqtt/actions/workflows/maven.yml)

This tool allows to control VOLTCRAFT SEM6000 BLE (bluetooth low energy) power measurement sockets via MQTT and therefore easy integration in the home automation of your choice!

Based on the great work of [magcode](https://github.com/magcode/sem6000-mqtt), [Heckie](https://github.com/Heckie75/voltcraft-sem-6000) and [hypfvieh](https://github.com/hypfvieh/bluez-dbus).

At the moment only crucial features are implemented: switching the relay, getting power and today's consumption values.

# Requirements
The tool only runs on Linux and requires Java and [Bluez](http://www.bluez.org/).

It has been tested on the following environments:
```
Raspberry Pi 3 Model B, Raspbian 10 Buster, OpenJDK 11.0.7, Bluez 5.50, built-in Bluetooth adapter usb:v1D6Bp0246d0532
Raspberry Pi 3 Model B, Raspbian 10 Buster, OpenJDK 11.0.7, Bluez 5.50, USB Bluetooth adapter 0a12:0001 Cambridge Silicon Radio, Ltd Bluetooth Dongle
Virtual machine, Ubuntu 20.04, OpenJDK 11.0.7, Bluez 5.54, Intel Bluetooth adapter 8087:0a2b
```
Problems occurred on Raspberry Pi Zero W.

# Configuration

You need a configuration file to configure the mqtt connection and the SEM6000 devices.
Configuration must be provided in yaml or property format.

Default configuration file names are 'sem2mqtt_bridge.yaml' or 'sem6.properties'.
Place the config in the same folder as the jar.

### YAML
Yet Another Markup Language (YAML) is the easiest way to configure the sem2mqtt bridge.

Example:
```yaml
mqtt:
  rootTopic: home/mysemdevices          # the prefix to use for the mqtt topics 
  url: tcp://localhost                  # url of the mqtt server
  clientId: client-for-sem6000          # client id to use for the mqtt connection
  username: mqttUsername                # optional username and
  password: mqttSecret                  #     password for the mqtt connection

sem:
  - mac: 00:00:00:00:00:01              # mac address of the sem 6000
    pin: 0000                           # pin of the device, 0000 is default
    name: sem1                          # name used in mqtt topic to identify the device
    updateInterval: PT10s               # How often shall updates be bridged to mqtt server
                                        #     Use "PT9d9h9m9s" format where d=days, h=hours, m=minutes, s=seconds
                                        #     (e.g. "PT1m30s" = 1 minute 30 seconds).
  - mac: 00:00:00:00:00:02
    pin: 1234
    name: sem2
    updateInterval: PT15m
```

### Property
Properties are antoher way to configure the bridge. The format is compatible with the version of [magcode](https://github.com/magcode/sem6000-mqtt).

```properties
# the mqtt root topic
rootTopic=home/mysemdevices
# IP or hostname of your mqtt broker
mqttServer=tcp://192.168.0.1
# Client Id when accessing mqtt broker
mqttClientId=client-for-sem6000
# Username and password to authenticate at mqtt broker. Leave both empty for unprotected broker  
mqttUsername=mqttUsername
mqttPassword=mqttSecret 

# the mac of your sem6000 device
sem1.mac=00:00:00:00:00:01
# the PIN of your sem6000 device. Default is 0000
sem1.pin=0000
# the name of your sem6000 device, use [a-z0-9]
sem1.name=sem1
# How often shall updates be bridged to the mqtt server in seconds.
sem1.refresh=60

sem2.mac=00:00:00:00:00:02
sem2.pin=0000
sem2.name=sem2
sem2.refresh=60
```

How to get the mac? Log on to your host, get into bluetooth console, activate scanning and list devices:
```shell
$ bluetoothctl
bluetooth$ scan on
bluetooth$ devices
```

# Running
It can simply be run with

```shell
$ java -jar sem6000-mqtt-1.0.0-jar-with-dependencies.jar [config_file_name]
```

Don't forget: Either add argument for config file name or place config with default file name beside jar.

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
A BLE connection is not stable sometimes. The tool will attempt to reconnect to the socket after five minutes in case the connection gets lost.
