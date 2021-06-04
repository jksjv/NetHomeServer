package nu.nethome.home.items.deconz;

import org.json.JSONObject;

public class ZHAConsumptionSensor extends Sensor {

	private String consumption;
	private String lastupdated;

	public ZHAConsumptionSensor(String id, JSONObject json) {
		super(id, json);
		JSONObject state = json.getJSONObject("state");
		this.consumption = Integer.toString(state.getInt("consumption"));
		this.lastupdated = state.getString("lastupdated");
	}

	public String getLastupdated() {
		return lastupdated;
	}

	public void setLastupdated(String lastupdated) {
		this.lastupdated = lastupdated;
	}

	public String getConsumption() {
		return consumption;
	}

	public void setConsumption(String consumption) {
		this.consumption = consumption;
	}

}
