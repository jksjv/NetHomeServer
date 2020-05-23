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
public class LightGroup {
    private final LightGroupState state;
    private final String type;
    private final String name;

    public LightGroup(LightGroupState state, String type, String name) {
        this.state = state;
        this.type = type;
        this.name = name;
    }

    public LightGroup(JSONObject json) {
        state = new LightGroupState(json.getJSONObject("state"));
        type = json.getString("type");
        name = json.getString("name");
    }

    public LightGroupState getState() {
        return state;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

}
