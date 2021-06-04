package nu.nethome.home.items.deconz;

import org.json.JSONObject;

public class ZHAPowerSensor extends Sensor {


	private String current;
	private String lastupdated;
	private String power;
	private String voltage;

	public ZHAPowerSensor(String id, JSONObject json) {
		super(id, json);
		JSONObject state = json.getJSONObject("state");
		this.current = Integer.toString(state.getInt("current"));
		this.lastupdated = state.getString("lastupdated");
		this.power = Integer.toString(state.getInt("power"));
		this.voltage = Integer.toString(state.getInt("voltage"));
	}

	public String getCurrent() {
		return current;
	}

	public void setCurrent(String current) {
		this.current = current;
	}

	public String getLastupdated() {
		return lastupdated;
	}

	public void setLastupdated(String lastupdated) {
		this.lastupdated = lastupdated;
	}

	public String getPower() {
		return power;
	}

	public void setPower(String power) {
		this.power = power;
	}

	public String getVoltage() {
		return voltage;
	}

	public void setVoltage(String voltage) {
		this.voltage = voltage;
	}

}
