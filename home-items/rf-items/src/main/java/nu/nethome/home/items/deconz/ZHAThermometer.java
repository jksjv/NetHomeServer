package nu.nethome.home.items.deconz;

import org.json.JSONObject;

public class ZHAThermometer extends Sensor {

	private String temperature;
	private String lastupdated;

	public ZHAThermometer(String id, JSONObject json) {
		super(id, json);
		JSONObject state = json.getJSONObject("state");
		this.setTemperature(Integer.toString(state.getInt("temperature")));
		this.lastupdated = state.getString("lastupdated");
	}

	public String getLastupdated() {
		return lastupdated;
	}

	public void setLastupdated(String lastupdated) {
		this.lastupdated = lastupdated;
	}

	public String getTemperature() {
		return temperature;
	}

	public void setTemperature(String temperature) {
		this.temperature = temperature;
	}


}
