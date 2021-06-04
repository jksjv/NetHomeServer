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
public class LightGroupState {
    private boolean allOn;
    private boolean anyOn;

    public LightGroupState() {
    	setAllOn(false);
    	setAnyOn(false);
    }

    public LightGroupState(JSONObject state) {
    	setAllOn(state.getBoolean("all_on"));
    	setAnyOn(state.getBoolean("any_on"));
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
