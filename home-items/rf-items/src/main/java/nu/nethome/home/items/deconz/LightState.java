/*
 * Copyright (C) 2005-2014, Stefan Strömberg <stefangs@nethome.nu>
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

import org.json.JSONObject;

/**
 *
 */
public class LightState {
    private boolean allOn;
    private boolean anyOn;
    private Integer brightness;
    private Integer hue;
    private Integer saturation;
    private Integer colorTemperature;

    public LightState(Integer brightness, Integer hue, Integer saturation) {
        allOn = true;
        this.brightness = brightness;
        this.hue = hue;
        this.saturation = saturation;
        this.colorTemperature = 0;
    }

    public LightState(Integer brightness, Integer colorTemperature) {
    	allOn = true;
        this.brightness = brightness;
        this.hue = -1;
        this.saturation = -1;
        this.colorTemperature = colorTemperature;
    }

    public LightState() {
        allOn = false;
    }

    public LightState(JSONObject state) {
    	allOn = state.getBoolean("on") && state.getBoolean("reachable");
        if (state.has("bri")) {
            brightness = state.getInt("bri");
        } else {
            brightness = allOn ? 100 : 0;
        }
        if (state.has("hue")) {
            hue = state.getInt("hue");
            saturation = state.getInt("sat");
        }
        if (state.has("colormode") && state.getString("colormode").equals("ct")) {
            this.colorTemperature = state.getInt("ct");
        }
    }

    public boolean isOn() {
        return allOn;
    }

    public Integer getBrightness() {
        return brightness;
    }

    public Integer getHue() {
        return hue;
    }

    public Integer getSaturation() {
        return saturation;
    }

    public Integer getColorTemperature() {
        return colorTemperature;
    }

    public boolean hasColorTemperature() {
    	if(colorTemperature==null) {
    		return false;
    	}
        return colorTemperature > 0;
    }

    public Boolean hasHueSat() {
    	if(saturation==null) {
    		return false;
    	}
        return saturation > 0;
    }

	public boolean isAllOn() {
		return allOn;
	}

	public void setAllOn(boolean allOn) {
		this.allOn = allOn;
	}

	public boolean isAnyOn() {
		return anyOn;
	}

	public void setAnyOn(boolean anyOn) {
		this.anyOn = anyOn;
	}
}
