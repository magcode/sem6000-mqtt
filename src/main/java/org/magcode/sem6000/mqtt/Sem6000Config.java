package org.magcode.sem6000.mqtt;

import org.json.JSONObject;

public class Sem6000Config {
	private String mac;
	private String pin;
	private String name;
	private int updateSeconds;

	public Sem6000Config() {
	}

	public Sem6000Config(String mac, String pin, String name, int updateSeconds) {
		this.mac = mac;
		this.pin = pin;
		this.name = name;
		this.updateSeconds = updateSeconds;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}

	public String getPin() {
		return pin;
	}

	public void setPin(String pin) {
		this.pin = pin;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getUpdateSeconds() {
		return updateSeconds;
	}

	public void setUpdateSeconds(int updateSeconds) {
		this.updateSeconds = updateSeconds;
	}

	public String getDiscoveryMessage(String rootTopic) {
		JSONObject discoveryMessageObj = new JSONObject();
		JSONObject cmps = new JSONObject();
		JSONObject cmpSwitch = new JSONObject();
		String mac_no_colon = mac.replace(":", "_");
		cmpSwitch.put("name", "switch");
		cmpSwitch.put("unique_id", "sw_"+name + "_" + mac_no_colon);
		cmpSwitch.put("command_topic", rootTopic + "/" + name + "/relay/set");
		cmpSwitch.put("state_topic", rootTopic + "/" + name + "/relay");
		cmpSwitch.put("payload_on", "true");
		cmpSwitch.put("payload_off", "false");
		cmpSwitch.put("state_on", "true");
		cmpSwitch.put("state_off", "false");
		cmpSwitch.put("p", "switch");
		cmpSwitch.put("device_class", "switch");
		cmps.put("switch", cmpSwitch);
		JSONObject cmpLed = new JSONObject();
		cmpLed.put("name", "led");
		cmpLed.put("unique_id", "led_"+name + "_" + mac_no_colon);
		cmpLed.put("command_topic", rootTopic + "/" + name + "/led/set");
		cmpLed.put("state_topic", rootTopic + "/" + name + "/led");
		cmpLed.put("payload_on", "true");
		cmpLed.put("payload_off", "false");
		cmpLed.put("state_on", "true");
		cmpLed.put("state_off", "false");
		cmpLed.put("p", "light");
		cmpLed.put("device_class", "light");
		cmps.put("led", cmpLed);
		JSONObject cmpVoltage = new JSONObject();
		cmpVoltage.put("name", "voltage");
		cmpVoltage.put("unique_id", "voltage_"+name + "_" + mac_no_colon);
		cmpVoltage.put("state_topic", rootTopic + "/" + name + "/voltage");
		cmpVoltage.put("unit_of_measurement", "V");
		cmpVoltage.put("p", "sensor");
		cmpVoltage.put("device_class", "voltage");
		cmps.put("voltage", cmpVoltage);
		JSONObject cmpPower = new JSONObject();
		cmpPower.put("name", "power");
		cmpPower.put("unique_id", "power_"+name + "_" + mac_no_colon);
		cmpPower.put("state_topic", rootTopic + "/" + name + "/power");
		cmpPower.put("unit_of_measurement", "W");
		cmpPower.put("p", "sensor");
		cmpPower.put("device_class", "power");
		cmpPower.put("state_class", "measurement");
		cmps.put("power", cmpPower);
		JSONObject cmpConsumption = new JSONObject();
		cmpConsumption.put("name", "consumption");
		cmpConsumption.put("unique_id", "consumption_"+name + "_" + mac_no_colon);
		cmpConsumption.put("state_topic", rootTopic + "/" + name + "/energytoday");
		cmpConsumption.put("unit_of_measurement", "Wh");
		cmpConsumption.put("p", "sensor");
		cmpConsumption.put("device_class", "energy");
		cmpConsumption.put("state_class", "total_increasing");
		cmps.put("consumption", cmpConsumption);
		JSONObject origin = new JSONObject();
		origin.put("name", "sem6000-mqtt");
		origin.put("sw", "1.0.3");
		origin.put("url", "https://github.com/magcode/sem6000-mqtt");
		discoveryMessageObj.put("o", origin);
		JSONObject device = new JSONObject();
		device.put("ids", mac_no_colon);
		device.put("name", "SEM 6000 " + name);
		device.put("mf", "Voltcraft");
		device.put("mdl", "Voltcraft SEM 6000");
		discoveryMessageObj.put("device", device);
		discoveryMessageObj.put("cmps", cmps);
		JSONObject availability = new JSONObject();
		availability.put("topic", rootTopic + "/" + name + "/state");
		availability.put("payload_available", "online");
		availability.put("payload_not_available", "lost");
		discoveryMessageObj.put("availability", availability);
		return discoveryMessageObj.toString();
	}
}
