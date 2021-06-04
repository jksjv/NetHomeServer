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

package nu.nethome.home.items.hue;

import nu.nethome.home.item.AutoCreationInfo;
import nu.nethome.home.item.HomeItemAdapter;
import nu.nethome.home.item.HomeItemType;
import nu.nethome.home.system.Event;
import nu.nethome.util.plugin.Plugin;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a Philips Hue bridge and handles communications with it
 */
@SuppressWarnings("UnusedDeclaration")
@Plugin
@HomeItemType(value = "Hardware", creationInfo = HueBridge.HueCreationInfo.class)
public class HueBridge extends HomeItemAdapter {

    public static final String UPN_P_CREATION_MESSAGE = "UPnP_Creation_Message";

    public static class HueCreationInfo implements AutoCreationInfo {
        static final String[] CREATION_EVENTS = {UPN_P_CREATION_MESSAGE};
        @Override
        public String[] getCreationEvents() {
            return CREATION_EVENTS;
        }

        @Override
        public boolean canBeCreatedBy(Event e) {
            return isPhilipsHueUPnPEvent(e);
        }

        @Override
        public String getCreationIdentification(Event e) {
            return String.format("Philips Hue bridge: \"%s\"",e.getAttribute("FriendlyName"));
        }
    }

    private static boolean isPhilipsHueUPnPEvent(Event e) {
        return e.getAttribute("ModelName").startsWith("Philips hue bridge");
    }

    private static final String MODEL = ("<?xml version = \"1.0\"?> \n"
            + "<HomeItem Class=\"HueBridge\"  Category=\"Hardware\" >"
            + "  <Attribute Name=\"State\" Type=\"String\" Get=\"getState\" Default=\"true\" />"
            + "  <Attribute Name=\"Address\" Type=\"String\" Get=\"getUrl\" Set=\"setUrl\" />"
            + "  <Attribute Name=\"Identity\" Type=\"String\" Get=\"getBridgeIdentity\" Init=\"setBridgeIdentity\" />"
            + "  <Attribute Name=\"DeviceName\" Type=\"String\" Get=\"getDeviceName\"  />"
            + "  <Attribute Name=\"SWVersion\" Type=\"String\" Get=\"getSWVersion\"  />"
            + "  <Attribute Name=\"UserName\" Type=\"String\" Get=\"getUserName\" Init=\"setUserName\" />"
            + "  <Attribute Name=\"RefreshInterval\" Type=\"String\" Get=\"getRefreshInterval\" Set=\"setRefreshInterval\" />"
            + "  <Action Name=\"findBridge\" Method=\"findBridge\" />"
            + "  <Action Name=\"registerUser\" Method=\"registerUser\" />"
            + "  <Action Name=\"reconnect\" Method=\"reconnect\" />"
            + "</HomeItem> ");
    public static final String ZGP_SWITCH_TYPE = "ZGPSwitch";

    private static Logger logger = Logger.getLogger(HueBridge.class.getName());

    private String userName = "";
    private String url = "";
    private String bridgeIdentity = "";
    private PhilipsHueBridge hueBridge;
    private int refreshInterval = 5;
    private int refreshCounter = 0;
    private HueConfig configuration = null;
    private String state = "Disconnected";


    @Override
    public String getModel() {
        return MODEL;
    }

    @Override
    public void activate() {
        reconnect();
    }

    public void reconnect() {
        hueBridge = new PhilipsHueBridge(url, bridgeIdentity);
        checkConnection();
    }

    public void findBridge() {
        try {
            List<PhilipsHueBridge> bridges = PhilipsHueBridge.listLocalPhilipsHueBridges();
            if (bridges.size() > 0) {
                this.url = bridges.get(0).getUrl();
                this.bridgeIdentity = bridges.get(0).getId();
                this.hueBridge = bridges.get(0);
            }
            checkConnection();
        } catch (IOException e) {
            logger.log(Level.INFO, "Failed to contact Hue location sevice", e);
        } catch (HueProcessingException e) {
            logger.log(Level.INFO, "Failed to contact Hue location sevice", e);
        }
    }

    private void checkConnection() {
        try {
            configuration = hueBridge.getConfiguration(userName);
            state = "Connected";
        } catch (IOException e) {
            this.state = "Disconnected";
            logger.log(Level.INFO, "Failed to contact HueBridge", e);
        } catch (HueProcessingException e) {
            this.state = "Disconnected";
            logger.log(Level.INFO, "Command failed in HueBridge", e);
        }
    }

    @Override
    public boolean receiveEvent(Event event) {
        if (!isActivated()) {
            return handleInit(event);
        }
        if (event.getAttribute(Event.EVENT_TYPE_ATTRIBUTE).equals("Hue_Message") &&
                event.getAttribute("Direction").equals("Out")) {
            String lampId = event.getAttribute("Hue.Lamp");
            String command = event.getAttribute("Hue.Command");
            if (command.equals("On") && lampId.length() > 0) {
                turnLampOn(lampId, event);
                reportLampState(lampId);
            } else if (command.equals("Off") && lampId.length() > 0) {
                turnLampOff(lampId);
                reportLampState(lampId);
            }
            return true;
        } else if (event.getAttribute(Event.EVENT_TYPE_ATTRIBUTE).equals("ReportItems") ||
                (event.getAttribute(Event.EVENT_TYPE_ATTRIBUTE).equals("MinuteEvent") && refreshCounter++ > refreshInterval)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    reportAllLampsState();
                }
            }).run();
            if (event.getAttribute(Event.EVENT_TYPE_ATTRIBUTE).equals("ReportItems")) {
                reportKnownSensors();
            }
            return true;
        }  else if (event.getAttribute(Event.EVENT_TYPE_ATTRIBUTE).equals("ReportHueLamp")) {
            String lampId = event.getAttribute("Hue.Lamp");
            reportLampState(lampId);
        } else if (event.getAttribute(Event.EVENT_TYPE_ATTRIBUTE).equals(UPN_P_CREATION_MESSAGE) &&
                isPhilipsHueUPnPEvent(event) &&
                event.getAttribute("SerialNumber").equals(getBridgeIdentity())) {
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
        refreshCounter = 0;
        try {
            List<LightId> ids = hueBridge.listLights(userName);
            for (LightId id : ids) {
                reportLampState(id.getLampId());
                Thread.sleep(100);
            }
        } catch (IOException e) {
            this.state = "Disconnected";
            logger.log(Level.INFO, "Failed to contact HueBridge", e);
        } catch (HueProcessingException e) {
            logger.log(Level.INFO, "Command failed in HueBridge", e);
        } catch (InterruptedException e) {
            // Do Dinada
        }
    }

    private void reportLampState(String lampId) {
        try {
            Light light = hueBridge.getLight(userName, lampId);
            Event event = server.createEvent("Hue_Message", "");
            event.setAttribute("Direction", "In");
            event.setAttribute("Hue.Lamp", lampId);
            event.setAttribute("Hue.Command", light.getState().isOn() ? "On" : "Off");
            event.setAttribute("Hue.Brightness", light.getState().getBrightness());
            event.setAttribute("Hue.Name", light.getName());
            event.setAttribute("Hue.Model", light.getModelid());
            event.setAttribute("Hue.Type", light.getType());
            event.setAttribute("Hue.Version", light.getSwversion());
            if (light.getState().hasHueSat()) {
                event.setAttribute("Hue.Hue", light.getState().getHue());
                event.setAttribute("Hue.Saturation", light.getState().getSaturation());
            }
            if (light.getState().hasColorTemperature()) {
                event.setAttribute("Hue.Temperature", light.getState().getColorTemperature());
            }
            server.send(event);
        } catch (IOException e) {
            this.state = "Disconnected";
            logger.log(Level.INFO, "Failed to contact HueBridge", e);
        } catch (HueProcessingException e) {
            logger.log(Level.INFO, "Command failed in HueBridge", e);
        }
    }

    private void reportKnownSensors() {
        try {
            List<Sensor> sensors = hueBridge.listSensors(userName);
            for (Sensor sensor : sensors) {
                if (sensor.getType().equals(ZGP_SWITCH_TYPE)) {
                    Event event = server.createEvent("Hue_Sensor_Message", "");
                    event.setAttribute("Direction", "In");
                    event.setAttribute("Hue.Id", sensor.getId());
                    event.setAttribute("Hue.Name", sensor.getName());
                    event.setAttribute("Hue.Model", sensor.getModelid());
                    event.setAttribute("Hue.Type", sensor.getType());
                    event.setAttribute("Hue.Manufacturername", sensor.getManufacturername());
                    server.send(event);
                }
            }
        } catch (IOException e) {
            this.state = "Disconnected";
            logger.log(Level.INFO, "Failed to contact HueBridge", e);
        } catch (HueProcessingException e) {
            logger.log(Level.INFO, "Command failed in HueBridge", e);
        }
    }


    private void turnLampOff(String lampId) {
        setLightState(lampId, new LightState());
    }

    private void setLightState(String lampId, LightState state) {
        try {
            hueBridge.setLightState(userName, lampId, state);
        } catch (IOException e) {
            this.state = "Disconnected";
            logger.log(Level.INFO, "Failed to contact HueBridge", e);
        } catch (HueProcessingException e) {
            logger.log(Level.INFO, "Command failed in HueBridge", e);
        }
    }

    private void turnLampOn(String lampId, Event event) {
        int brightness = event.getAttributeInt("Hue.Brightness");
        int temperature = event.getAttributeInt("Hue.Temperature");
        int hue = event.getAttributeInt("Hue.Hue");
        int saturation = event.getAttributeInt("Hue.Saturation");
        LightState state;
        if (temperature > 0) {
            state = new LightState(brightness, temperature);
        } else {
            state = new LightState(brightness, hue, saturation);
        }
        setLightState(lampId, state);
    }

    public void registerUser() {
        try {
            String result = hueBridge.registerUser("OpenNetHomeServer");
            userName = result;
            checkConnection();
        } catch (IOException e) {
            this.state = "Disconnected";
            logger.log(Level.INFO, "Failed to contact HueBridge", e);
        } catch (HueProcessingException e) {
            this.state = "Disconnected";
            logger.log(Level.INFO, "Command failed in HueBridge", e);
        }
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        hueBridge = new PhilipsHueBridge(url, "");
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

}
