package nu.nethome.home.items.deconz;

public class DeconzConstants {

	public static final String DE_CONZ_SENSOR_MESSAGE = "deCONZ_sensor_Message";
	
	public static final String DE_CONZ_ID = "deCONZ.Id";
	
	public static final String ZGP_SWITCH_TYPE = "ZGPSwitch";
	public static final String ZHA_CONSUMPTION ="ZHAConsumption";
	public static final String ZHA_POWER = "ZHAPower";
	public static final String ZHA_TEMPERATURE = "ZHATemperature";
	public static final String ZHA_HUMIDITY = "ZHAHumidity";
	
	public static final String DE_CONZ_SENSOR_MESSAGE_ZHA_POWER = DE_CONZ_SENSOR_MESSAGE + "_" + ZHA_POWER;
	public static final String DE_CONZ_SENSOR_MESSAGE_ZHA_CONSUMPTION = DE_CONZ_SENSOR_MESSAGE + "_" + ZHA_CONSUMPTION;
	public static final String DE_CONZ_SENSOR_MESSAGE_ZHA_THERMOMETER = DE_CONZ_SENSOR_MESSAGE + "_" + ZHA_TEMPERATURE;
	public static final String DE_CONZ_SENSOR_MESSAGE_ZHA_HUMIDITY = DE_CONZ_SENSOR_MESSAGE + "_" + ZHA_HUMIDITY;

	private DeconzConstants() {	}
}
