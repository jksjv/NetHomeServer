package nu.nethome.home.items.deconz.zhatemperature;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import nu.nethome.home.item.AutoCreationInfo;
import nu.nethome.home.item.ExtendedLoggerComponent;
import nu.nethome.home.item.HomeItem;
import nu.nethome.home.item.HomeItemAdapter;
import nu.nethome.home.item.HomeItemType;
import nu.nethome.home.item.ValueItem;
import nu.nethome.home.system.Event;
import nu.nethome.util.plugin.Plugin;

import static nu.nethome.home.items.deconz.DeconzConstants.*;

@Plugin
@HomeItemType(value = "Thermometers", creationInfo = DeconzZHAThermometer.deCONZCreationInfo.class)
public class DeconzZHAThermometer extends HomeItemAdapter implements HomeItem, ValueItem {

	private static Logger logger = Logger.getLogger(DeconzZHAThermometer.class.getName());

	private String temperature = "";
	private ExtendedLoggerComponent tempLoggerComponent = new ExtendedLoggerComponent(this);
	private String itemDeviceId = "";
	private static final SimpleDateFormat dateFormatterOut = new SimpleDateFormat("HH:mm:ss yyyy.MM.dd ");
	private static final SimpleDateFormat dateFormatterIn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	protected boolean hasBeenUpdated = false;
	private Date latestUpdateOrCreation = new Date();

	private String sensorModel;
	private String type;
	private String version;
	private String manufacturer;

	public static class deCONZCreationInfo implements AutoCreationInfo {

		static final String[] CREATION_EVENTS = { DE_CONZ_SENSOR_MESSAGE_ZHA_THERMOMETER };

		@Override
		public String[] getCreationEvents() {
			return CREATION_EVENTS;
		}

		@Override
		public boolean canBeCreatedBy(Event e) {
			return e.isType(DE_CONZ_SENSOR_MESSAGE_ZHA_THERMOMETER);
		}

		@Override
		public String getCreationIdentification(Event e) {
			return String.format("deCONZ ZHATemperature Temperature %s: \"%s\"", e.getAttribute("deCONZ.temperature"),
					e.getAttribute("deCONZ.Name"));
		}
	}

	private static final String MODEL = ("<?xml version = \"1.0\"?> \n"
			+ "<HomeItem Class=\"deCONZ ZHATemperature Current\" Category=\"Thermometers\" >"
			+ "  <Attribute Name=\"Temperature\" 	Type=\"String\" Get=\"getValue\" Default=\"true\"  Unit=\"°C\" />"
			+ "  <Attribute Name=\"SensorModel\" Type=\"String\" Get=\"getSensorModel\" 	Init=\"setSensorModel\" />"
			+ "  <Attribute Name=\"Type\" Type=\"String\" Get=\"getType\" 	Init=\"setType\" />"
			+ "  <Attribute Name=\"Version\" Type=\"String\" Get=\"getVersion\" 	Init=\"setVersion\" />"
			+ "  <Attribute Name=\"Manufacturer\" Type=\"String\" Get=\"getManufacturer\" 	Init=\"setManufacturer\" />"
			+ "  <Attribute Name=\"TimeSinceUpdate\" 	Type=\"String\" Get=\"getTimeSinceUpdate\" />"
			+ "  <Attribute Name=\"DeviceId\" Type=\"String\" Get=\"getDeviceId\" 	Set=\"setDeviceId\" />"
			+ "  <Attribute Name=\"LogFile\" Type=\"String\" Get=\"getLogFile\" 	Set=\"setLogFile\" />"
			+ "  <Attribute Name=\"LastUpdate\" Type=\"String\" Get=\"getLastUpdate\"  Unit=\"s\" />" + "</HomeItem> ");

	@Override
	protected boolean initAttributes(Event event) {
		itemDeviceId = event.getAttribute(DE_CONZ_ID);
		this.type = event.getAttribute("deCONZ.Type");
		this.version = event.getAttribute("deCONZ.Version");
		this.sensorModel = event.getAttribute("deCONZ.Model");
//		this.name = event.getAttribute("deCONZ.Name");
		this.manufacturer = event.getAttribute("deCONZ.Manufacturername");
		setTemperature(String.format("%.1f", (event.getAttributeInt("deCONZ.temperature") * 0.01)));

		return true;
	}

	public boolean receiveEvent(Event event) {
		if ((event.getAttribute(Event.EVENT_TYPE_ATTRIBUTE).equals(DE_CONZ_SENSOR_MESSAGE_ZHA_THERMOMETER) ||event.getAttribute(Event.EVENT_TYPE_ATTRIBUTE).equals(DE_CONZ_SENSOR_MESSAGE)) 
				&& event.getAttribute("Direction").equals("In")
				&& event.getAttribute(DE_CONZ_ID).equals(itemDeviceId)) {
			setTemperature(String.format("%.1f", (event.getAttributeInt("deCONZ.temperature") * 0.01)));
			hasBeenUpdated = true;
			try {
				latestUpdateOrCreation = dateFormatterIn.parse(event.getAttribute("deCONZ.lastupdated"));
			} catch (ParseException e) {
				logger.severe(e.getMessage());
			}
			return true;
		}
		return handleInit(event);
	}

	public String getDeviceId() {
		return itemDeviceId;
	}

	public void setDeviceId(String deviceId) {
		itemDeviceId = deviceId;
	}

	public String getLastUpdate() {
		return hasBeenUpdated ? dateFormatterOut.format(latestUpdateOrCreation) : "";
	}

	public String getLogFile() {
		return tempLoggerComponent.getFileName();
	}

	public void setLogFile(String logfile) {
		tempLoggerComponent.setFileName(logfile);
	}

	public String getTimeSinceUpdate() {
		return Long.toString((new Date().getTime() - latestUpdateOrCreation.getTime()) / 1000);
	}

	@Override
	public String getValue() {
		return getTemperature();
	}

	@Override
	public String getModel() {
		return MODEL;
	}

	public String getSensorModel() {
		return sensorModel;
	}

	public void setSensorModel(String sensorModel) {
		this.sensorModel = sensorModel;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}

	public String getTemperature() {
		return temperature;
	}

	public void setTemperature(String temperature) {
		this.temperature = temperature;
	}

}
