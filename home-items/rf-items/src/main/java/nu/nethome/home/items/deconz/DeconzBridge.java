/*
 * Copyright (C) 2005-2013, Stefan Strömberg <stefangs@nethome.nu>
 *
 * This file is part of OpenNetHome  (http://www.nethome.nu)
 *
 * OpenNetHome is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenNetHome is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nu.nethome.home.items.deconz;

import static nu.nethome.home.items.deconz.DeconzConstants.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import nu.nethome.home.item.AutoCreationInfo;
import nu.nethome.home.item.HomeItemAdapter;
import nu.nethome.home.item.HomeItemType;
import nu.nethome.home.items.fineoffset.FineOffsetThermometer;
import nu.nethome.home.items.ikea.JSONData;
import nu.nethome.home.system.Event;
import nu.nethome.home.system.HomeService;
import nu.nethome.util.plugin.Plugin;

/**
 * Represents a deCONZ bridge and handles communications with it
 */
@SuppressWarnings("UnusedDeclaration")
@Plugin
@HomeItemType(value = "Hardware", creationInfo = DeconzBridge.DeconzCreationInfo.class)
public class DeconzBridge extends HomeItemAdapter {

	public static final String UPN_P_CREATION_MESSAGE = "UPnP_Creation_Message";

	public static class DeconzCreationInfo implements AutoCreationInfo {
		static final String[] CREATION_EVENTS = { UPN_P_CREATION_MESSAGE };

		@Override
		public String[] getCreationEvents() {
			return CREATION_EVENTS;
		}

		@Override
		public boolean canBeCreatedBy(Event e) {
			return isDeCONZUPnPEvent(e);
		}

		@Override
		public String getCreationIdentification(Event e) {
			return String.format("deCONZ bridge: \"%s\"", e.getAttribute("FriendlyName"));
		}
	}

	private static boolean isDeCONZUPnPEvent(Event e) {
		return e.getAttribute("ModelName").startsWith("deCONZ bridge");
	}

	private static final String MODEL = ("<?xml version = \"1.0\"?> \n"
			+ "<HomeItem Class=\"DeCONZBridge\"  Category=\"Hardware\" >"
			+ "  <Attribute Name=\"State\" Type=\"String\" Get=\"getState\" Default=\"true\" />"
			+ "  <Attribute Name=\"Address\" Type=\"String\" Get=\"getUrl\" Set=\"setUrl\" />"
			+ "  <Attribute Name=\"Identity\" Type=\"String\" Get=\"getBridgeIdentity\" Init=\"setBridgeIdentity\" />"
			+ "  <Attribute Name=\"DeviceName\" Type=\"String\" Get=\"getDeviceName\"  />"
			+ "  <Attribute Name=\"SWVersion\" Type=\"String\" Get=\"getSWVersion\"  />"
			+ "  <Attribute Name=\"Token\" Type=\"String\" Get=\"getToken\" Init=\"setToken\" />"
			+ "  <Attribute Name=\"RefreshInterval\" Type=\"String\" Get=\"getRefreshInterval\" Set=\"setRefreshInterval\" />"
			+ "  <Action Name=\"registerUser\" Method=\"registerUser\" />"
			+ "  <Action Name=\"reconnect\" Method=\"reconnect\" />" + "</HomeItem> ");

	private static Logger logger = Logger.getLogger(DeconzBridge.class.getName());

	private String token = "";
	private String url = "";
	private String bridgeIdentity = "";
	private int refreshInterval = 5;
	private int refreshCounter = 0;
	private DeconzConfig configuration = null;
	private String state = "Disconnected";

	JsonRestClient client = new JsonRestClient();
	private DeconzWebsocketClient webSocketClient;

	@Override
	public String getModel() {
		return MODEL;
	}

	@Override
	public void activate() {
		reconnect();
	}

	public void reconnect() {
		checkConnection();
		try {
			if (!url.startsWith("http")) {
				webSocketClient = new DeconzWebsocketClient(new URI("ws://" + url + ":443"));
			} else {
				webSocketClient = new DeconzWebsocketClient(new URI("ws" + url.substring(4) + ":443"));
			}

			webSocketClient.connect();
		} catch (URISyntaxException e) {
			logger.severe(e.getMessage());
		}
	}

	private void checkConnection() {
		try {
			String resource = String.format("/api/%s/config", token);
			if (!url.startsWith("http")) {
				url = "http://" + url;
			}
			JSONData jResult = client.get(url, resource, null);
			checkForErrorResponse(jResult);
			configuration = new DeconzConfig(jResult.getObject());
			this.state = "Connected";
		} catch (IOException e) {
			this.state = "Disconnected";
			logger.log(Level.INFO, "Failed to contact DeCONZBridge", e);
		} catch (JSONException | DeconzProcessingException e) {
			this.state = "Disconnected";
			logger.log(Level.INFO, "Command failed in DeCONZBridge", e);
		}
	}

	private void checkForErrorResponse(JSONData result) throws DeconzProcessingException {
		if (result.isObject() && result.getObject().has("error")) {
			JSONObject error = result.getObject().getJSONObject("error");
			throw new DeconzProcessingException(error.getString("description"), error.getInt("type"));
		}
	}

	@Override
	public boolean receiveEvent(Event event) {
		if (!isActivated()) {
			return handleInit(event);
		}
		if (event.getAttribute(Event.EVENT_TYPE_ATTRIBUTE).equals("deCONZ_Message")
				&& event.getAttribute("Direction").equals("Out")) {
			String lampId = event.getAttribute(DE_CONZ_ID);
			String command = event.getAttribute("deCONZ.Command");
			if (command.equals("On") && lampId.length() > 0) {
				turnLampOn(lampId, event);
				reportLampState(lampId);
			} else if (command.equals("Off") && lampId.length() > 0) {
				turnLampOff(lampId);
				reportLampState(lampId);
			}
			return true;
		} else if (event.getAttribute(Event.EVENT_TYPE_ATTRIBUTE).equals("deCONZ_lightgroup")
				&& event.getAttribute("Direction").equals("Out")) {
			String lampId = event.getAttribute(DE_CONZ_ID);
			String command = event.getAttribute("deCONZ.Command");
			if (command.equals("On") && lampId.length() > 0) {
				turnLampGroupOn(lampId, event);
//				reportLampGroupState(lampId);
			} else if (command.equals("Off") && lampId.length() > 0) {
				turnLampGroupOff(lampId);
//				reportLampGroupState(lampId);
			}
			return true;
		} else if (event.getAttribute(Event.EVENT_TYPE_ATTRIBUTE).equals("ReportItems")
				|| (event.getAttribute(Event.EVENT_TYPE_ATTRIBUTE).equals("MinuteEvent")
						&& refreshCounter++ > refreshInterval)) {
			reportAllLampsState();
			reportAllGroupsState();
			if (event.getAttribute(Event.EVENT_TYPE_ATTRIBUTE).equals("ReportItems")) {
				reportKnownSensors();
			}
			return true;
		} else if (event.getAttribute(Event.EVENT_TYPE_ATTRIBUTE).equals("ReportdeCONZLamp")) {
			String lampId = event.getAttribute(DE_CONZ_ID);
			reportLampState(lampId);
		} else if (event.getAttribute(Event.EVENT_TYPE_ATTRIBUTE).equals(UPN_P_CREATION_MESSAGE)
				&& isDeCONZUPnPEvent(event) && event.getAttribute("SerialNumber").equals(getBridgeIdentity())) {
			setUrl(extractBaseUrl(event.getAttribute("Location")));
			return true;
		}
		return false;
	}

	@Override
	protected boolean initAttributes(Event event) {
		setUrl(extractBaseUrl(event.getAttribute("Location")));
		setBridgeIdentity(event.getAttribute("SerialNumber"));
		return true;
	}

	private String extractBaseUrl(String url) {
		int pos = url.indexOf("/", 9);
		if (pos > 0) {
			return url.substring(0, pos);
		}
		return url;
	}

	private void reportAllLampsState() {
		try {
			List<LightId> ids = listaAllLights();
			for (LightId id : ids) {
				reportLampState(id.getLampId());
			}
		} catch (IOException e) {
			this.state = "Disconnected";
			logger.log(Level.INFO, "Failed to contact deCONZBridge", e);
		} catch (JSONException e) {
			logger.log(Level.INFO, "Command failed in deCONZBridge", e);
		}
	}
	
	private void reportAllGroupsState() {
		try {
			List<LightId> ids = listaAllGroups();
			for (LightId id : ids) {
				reportGroupState(id.getLampId());
			}
		} catch (IOException e) {
			this.state = "Disconnected";
			logger.log(Level.INFO, "Failed to contact deCONZBridge", e);
		} catch (JSONException e) {
			logger.log(Level.INFO, "Command failed in deCONZBridge", e);
		}
	}

	private void reportLampState(String lampId) {
		try {
			Light light = getLight(lampId);
			Event event = server.createEvent("deCONZ_Message", "");
			event.setAttribute("Direction", "In");
			event.setAttribute(DE_CONZ_ID, lampId);
			event.setAttribute("deCONZ.Command", light.getState().isOn() ? "On" : "Off");
			event.setAttribute("deCONZ.Brightness", light.getState().getBrightness());
			event.setAttribute("deCONZ.Name", light.getName());
			event.setAttribute("deCONZ.Model", light.getModelid());
			event.setAttribute("deCONZ.Type", light.getType());
			event.setAttribute("deCONZ.Version", light.getSwversion());
			if (light.getState().hasHueSat()) {
				event.setAttribute("deCONZ.Hue", light.getState().getHue());
				event.setAttribute("deCONZ.Saturation", light.getState().getSaturation());
			}
			if (light.getState().hasColorTemperature()) {
				event.setAttribute("deCONZ.Temperature", light.getState().getColorTemperature());
			}
			server.send(event);
		} catch (IOException e) {
			this.state = "Disconnected";
			logger.log(Level.INFO, "Failed to contact deCONZBridge", e);
		} catch (JSONException e) {
			logger.log(Level.INFO, "Command failed in deCONZBridge", e);
		}
	}
	
	private void reportGroupState(String lampId) {
		try {
			LightGroup light = getLightGroup(lampId);
			Event event = server.createEvent("deCONZ_lightgroup", "");
			event.setAttribute("Direction", "In");
			event.setAttribute(DE_CONZ_ID, lampId);
			event.setAttribute("deCONZ.Command", light.getState().isAllOn() ? "On" : "Off");
			event.setAttribute("deCONZ.Name", light.getName());
			event.setAttribute("deCONZ.Type", light.getType());
			server.send(event);
		} catch (IOException e) {
			this.state = "Disconnected";
			logger.log(Level.INFO, "Failed to contact deCONZBridge", e);
		} catch (JSONException e) {
			logger.log(Level.INFO, "Command failed in deCONZBridge", e);
		}
	}

	private Light getLight(String lampId) throws IOException {
		String resource = String.format("/api/%s/lights/%s", token, lampId);
		JSONData jResult = client.get(url, resource, null);
		return new Light(jResult.getObject());
	}
	
	private LightGroup getLightGroup(String lampId) throws IOException {
		String resource = String.format("/api/%s/groups/%s", token , lampId);
		JSONData jResult = client.get(url, resource, null);
		return new LightGroup(jResult.getObject());
	}

	private ZHAPowerSensor getZHAPowerSensor(String id) throws IOException {
		String resource = getSensor(id);
		JSONData jResult = client.get(url, resource, null);
		return new ZHAPowerSensor(id, jResult.getObject());
	}

	private ZHAConsumptionSensor getZHAConsumptionSensor(String id) throws IOException {
		String resource = getSensor(id);
		JSONData jResult = client.get(url, resource, null);
		return new ZHAConsumptionSensor(id, jResult.getObject());
	}

	private ZHAThermometer getZHAThermometer(String id) throws IOException {
		String resource = getSensor(id);
		JSONData jResult = client.get(url, resource, null);
		return new ZHAThermometer(id, jResult.getObject());
	}
	
	private String getSensor(String id) {
		String resource = String.format("/api/%s/sensors/" + id, token);
		return resource;
	}

	private void reportKnownSensors() {

		if (!url.startsWith("http")) {
			url = "http://" + url;
		}
		try {
			List<Sensor> sensors = listaAllSensors();
			for (Sensor sensor : sensors) {
				if (sensor.getType().equals(ZGP_SWITCH_TYPE)) {
					Event event = server.createEvent(DE_CONZ_SENSOR_MESSAGE, "");
					event.setAttribute("Direction", "In");
					event.setAttribute(DE_CONZ_ID, sensor.getId());
					event.setAttribute("deCONZ.Name", sensor.getName());
					event.setAttribute("deCONZ.Model", sensor.getModelid());
					event.setAttribute("deCONZ.Type", sensor.getType());
					event.setAttribute("deCONZ.Manufacturername", sensor.getManufacturername());
					server.send(event);
				} else if (sensor.getType().equals(ZHA_POWER)) {
					ZHAPowerSensor zhaSensor = getZHAPowerSensor(sensor.getId());
					Event event = server.createEvent(DE_CONZ_SENSOR_MESSAGE, "");
					event.setAttribute("Direction", "In");
					event.setAttribute(DE_CONZ_ID, zhaSensor.getId());
					event.setAttribute("deCONZ.Name", zhaSensor.getName());
					event.setAttribute("deCONZ.Model", zhaSensor.getModelid());
					event.setAttribute("deCONZ.Type", zhaSensor.getType());
					event.setAttribute("deCONZ.Version", zhaSensor.getSwversion());
					event.setAttribute("deCONZ.Manufacturername", zhaSensor.getManufacturername());
					event.setAttribute("deCONZ.current", zhaSensor.getCurrent());
					event.setAttribute("deCONZ.power", zhaSensor.getPower());
					event.setAttribute("deCONZ.voltage", zhaSensor.getVoltage());
					event.setAttribute("deCONZ.lastupdated", zhaSensor.getLastupdated());
					server.send(event);
				} else if (sensor.getType().equals(ZHA_CONSUMPTION)) {
					ZHAConsumptionSensor zhaSensor = getZHAConsumptionSensor(sensor.getId());
					Event event = server.createEvent(DE_CONZ_SENSOR_MESSAGE_ZHA_CONSUMPTION, "");
					event.setAttribute("Direction", "In");
					event.setAttribute(DE_CONZ_ID, zhaSensor.getId());
					event.setAttribute("deCONZ.Name", zhaSensor.getName());
					event.setAttribute("deCONZ.Model", zhaSensor.getModelid());
					event.setAttribute("deCONZ.Type", zhaSensor.getType());
					event.setAttribute("deCONZ.Version", zhaSensor.getSwversion());
					event.setAttribute("deCONZ.Manufacturername", zhaSensor.getManufacturername());
					event.setAttribute("deCONZ.consumption", zhaSensor.getConsumption());
					event.setAttribute("deCONZ.lastupdated", zhaSensor.getLastupdated());
					server.send(event);
				} else if (sensor.getType().equals(ZHA_TEMPERATURE)) {
					ZHAThermometer zhaSensor = getZHAThermometer(sensor.getId());
					Event event = server.createEvent(DE_CONZ_SENSOR_MESSAGE_ZHA_THERMOMETER, "");
					event.setAttribute("Direction", "In");
					event.setAttribute(DE_CONZ_ID, zhaSensor.getId());
					event.setAttribute("deCONZ.Model", zhaSensor.getModelid());
					event.setAttribute("deCONZ.Name", zhaSensor.getName());
					event.setAttribute("deCONZ.Type", zhaSensor.getType());
					event.setAttribute("deCONZ.Version", zhaSensor.getSwversion());
					event.setAttribute("deCONZ.Manufacturername", zhaSensor.getManufacturername());
					event.setAttribute("deCONZ.temperature", zhaSensor.getTemperature());
					event.setAttribute("deCONZ.lastupdated", zhaSensor.getLastupdated());
					server.send(event);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private List<Sensor> listaAllSensors() throws IOException {
		String resource = String.format("/api/%s/sensors", token);
		List<Sensor> sensors = new ArrayList<Sensor>();
		JSONData jResult = client.get(url, resource, null);
		JSONObject obj = jResult.getObject();
		String[] names = JSONObject.getNames(obj);
		for (String name : names) {
			sensors.add(new Sensor(name, obj.getJSONObject(name)));
		}
		return sensors;
	}

	private List<LightId> listaAllLights() throws IOException {
		String resource = String.format("/api/%s/lights", token);
		List<LightId> lights = new ArrayList<LightId>();
		JSONData jResult = client.get(url, resource, null);
		JSONObject obj = jResult.getObject();
		String[] names = JSONObject.getNames(obj);
		for (String name : names) {
			lights.add(new LightId(name, obj.getJSONObject(name).getString("name")));
		}
		return lights;
	}

	private List<LightId> listaAllGroups() throws IOException {
		String resource = String.format("/api/%s/groups", token);
		List<LightId> lights = new ArrayList<LightId>();
		JSONData jResult = client.get(url, resource, null);
		JSONObject obj = jResult.getObject();
		String[] names = JSONObject.getNames(obj);
		for (String name : names) {
			lights.add(new LightId(name, obj.getJSONObject(name).getString("name")));
		}
		return lights;
	}
	
	private void turnLampOff(String lampId) {
		setLightState(lampId, new LightState());
	}
	
	private void turnLampGroupOff(String lampId) {
		setLightGroupState(lampId, new LightState());
	}

	private void setLightState(String lampId, LightState state) {
		JSONObject stateParameter = new JSONObject();
		if (state.isOn()) {
			stateParameter.put("on", true);
			stateParameter.put("bri", state.getBrightness());
			if (state.hasColorTemperature()) {
				stateParameter.put("ct", state.getColorTemperature());
			} else if (state.hasHueSat()) {
				stateParameter.put("hue", state.getHue());
				stateParameter.put("sat", state.getSaturation());
			}
		} else {
			stateParameter.put("on", false);
		}
		String resource = String.format("/api/%s/lights/%s/state", token, lampId);
		try {
			JSONData result = client.put(url, resource, stateParameter);
			checkForErrorResponse(result);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DeconzProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void setLightGroupState(String lampId, LightState state) {
		JSONObject stateParameter = new JSONObject();
		if (state.isOn()) {
			stateParameter.put("on", true);
			stateParameter.put("bri", state.getBrightness());
			if (state.hasColorTemperature()) {
				stateParameter.put("ct", state.getColorTemperature());
			} else if (state.hasHueSat()) {
				stateParameter.put("hue", state.getHue());
				stateParameter.put("sat", state.getSaturation());
			}
		} else {
			stateParameter.put("on", false);
		}
		String resource = String.format("/api/%s/groups/%s/action", token, lampId);
		try {
			JSONData result = client.put(url, resource, stateParameter);
			checkForErrorResponse(result);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DeconzProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void turnLampOn(String lampId, Event event) {
		Integer brightness = null;
		if(event.getAttribute("deCONZ.Brightness")!="") {
			 brightness = event.getAttributeInt("deCONZ.Brightness");
		}
		
		Integer temperature = event.getAttributeInt("deCONZ.Temperature");
		Integer hue = event.getAttributeInt("deCONZ.Hue");
		Integer saturation = event.getAttributeInt("deCONZ.Saturation");
		LightState state;
		if (temperature > 0) {
			state = new LightState(brightness, temperature);
		} else {
			state = new LightState(brightness, hue, saturation);
		}
		setLightState(lampId, state);
	}

	private void turnLampGroupOn(String lampId, Event event) {
		int brightness = event.getAttributeInt("deCONZ.Brightness");
		int temperature = event.getAttributeInt("deCONZ.Temperature");
		int hue = event.getAttributeInt("deCONZ.Hue");
		int saturation = event.getAttributeInt("deCONZ.Saturation");
		LightState state;
		if (temperature > 0) {
			state = new LightState(brightness, temperature);
		} else {
			state = new LightState(brightness, hue, saturation);
		}
		setLightGroupState(lampId, state);
	}
	public void registerUser() {
//        try {
//            String result = hueBridge.registerUser("OpenNetHomeServer");
//            userName = result;
//            checkConnection();
//        } catch (IOException e) {
//            this.state = "Disconnected";
//            logger.log(Level.INFO, "Failed to contact HueBridge", e);
//        } catch (HueProcessingException e) {
//            this.state = "Disconnected";
//            logger.log(Level.INFO, "Command failed in HueBridge", e);
//        }
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getBridgeIdentity() {
		return bridgeIdentity;
	}

	public void setBridgeIdentity(String bridgeIdentity) {
		this.bridgeIdentity = bridgeIdentity;
	}

	public String getRefreshInterval() {
		return Integer.toString(refreshInterval);

	}

	public void setRefreshInterval(String refreshInterval) {
		this.refreshInterval = Integer.parseInt(refreshInterval);
		refreshCounter = this.refreshInterval + 1;
	}

	public String getState() {
		return state;
	}

	public String getSWVersion() {
		return configuration != null ? configuration.getSwVersion() : "";
	}

	public String getDeviceName() {
		return configuration != null ? configuration.getName() : "";
	}

	public class DeconzWebsocketClient extends WebSocketClient {

		public DeconzWebsocketClient(URI serverUri, Draft draft) {
			super(serverUri, draft);
		}

		public DeconzWebsocketClient(URI serverURI) {
			super(serverURI);
		}

		public DeconzWebsocketClient(URI serverUri, Map<String, String> httpHeaders) {
			super(serverUri, httpHeaders);
		}

		@Override
		public void onOpen(ServerHandshake handshakedata) {
			System.out.println("opened connection");
			// if you plan to refuse connection based on ip or httpfields overload:
			// onWebsocketHandshakeReceivedAsClient
		}

		@Override
		public void onMessage(String message) {
			JSONData data = new JSONData(message);
			JSONObject jObject = data.getObject();
			Event event;
			String type = jObject.getString("r");
			if (type.equalsIgnoreCase("sensors")) {
				event = server.createEvent(DE_CONZ_SENSOR_MESSAGE, "");
			} else if (type.equalsIgnoreCase("groups")) {
				event = server.createEvent("deCONZ_lightgroup", "");
			} else {
				event = server.createEvent("deCONZ_Message", "");
			}
			event.setAttribute(DE_CONZ_ID, jObject.getString("id"));
			event.setAttribute("Direction", "In");

			try {
			JSONObject state = jObject.getJSONObject("state");
			String[] names = JSONObject.getNames(state);
			for (String name : names) {
				try {
					event.setAttribute("deCONZ." + name, state.getString(name));
				} catch (JSONException je) {
					try {
						event.setAttribute("deCONZ." + name, Integer.toString(state.getInt(name)));
					} catch (JSONException je2) {
						event.setAttribute("deCONZ." + name, Boolean.toString(state.getBoolean(name)));
					}
				}
			}
			} catch (JSONException je) {
				logger.finest("Json object with no state object");
			}
			server.send(event);
		}

		@Override
		public void onClose(int code, String reason, boolean remote) {
			// The codecodes are documented in class org.java_websocket.framing.CloseFrame
			logger.info("Connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code + " Reason: "
					+ reason);
			state = "Disconnected";
		}

		@Override
		public void onError(Exception ex) {
			ex.printStackTrace();
			// if the error is fatal then onClose will be called additionally
		}
	}
}
