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

import nu.nethome.home.item.AutoCreationInfo;
import nu.nethome.home.item.HomeItem;
import nu.nethome.home.item.HomeItemAdapter;
import nu.nethome.home.item.HomeItemType;
import nu.nethome.home.system.Event;
import nu.nethome.util.plugin.Plugin;
import static nu.nethome.home.items.deconz.DeconzConstants.*;

@SuppressWarnings("UnusedDeclaration")
@Plugin
@HomeItemType(value = "Lamps", creationInfo = DeconzLamp.deCONZCreationInfo.class)
public class DeconzLamp extends HomeItemAdapter implements HomeItem {

	public static class deCONZCreationInfo implements AutoCreationInfo {
		static final String[] CREATION_EVENTS = { "deCONZ_Message" };

		@Override
		public String[] getCreationEvents() {
			return CREATION_EVENTS;
		}

		@Override
		public boolean canBeCreatedBy(Event e) {
			return e.isType("deCONZ_Message");
		}

		@Override
		public String getCreationIdentification(Event e) {
			return String.format("deCONZ lamp %s: \"%s\"", e.getAttribute(DE_CONZ_ID),
					e.getAttribute("deCONZ.Name"));
		}
	}

	public static final int DIM_STEP = 20;
	private String lampId = "";
	private boolean isOn;

	private static final String MODEL = ("<?xml version = \"1.0\"?> \n"
			+ "<HomeItem Class=\"deCONZLamp\" Category=\"Lamps\" >"
			+ "  <Attribute Name=\"State\" Type=\"String\" Get=\"getState\" Default=\"true\" />"
			+ "  <Attribute Name=\"Identity\" Type=\"String\" Get=\"getLampId\" 	Set=\"setLampId\" />"
			+ "  <Attribute Name=\"LampModel\" Type=\"String\" Get=\"getLampModel\" 	Init=\"setLampModel\" />"
			+ "  <Attribute Name=\"Type\" Type=\"String\" Get=\"getLampType\" 	Init=\"setLampType\" />"
			+ "  <Attribute Name=\"Version\" Type=\"String\" Get=\"getLampVersion\" 	Init=\"setLampVersion\" />"
			+ "  <Attribute Name=\"Brightness\" Type=\"String\" Get=\"getCurrentBrightness\"  />"
			+ "  <Attribute Name=\"OnBrightness\" Type=\"String\" Get=\"getBrightness\" 	Set=\"setBrightness\" />"
			+ "  <Attribute Name=\"Color\" Type=\"String\" Get=\"getColor\" 	Set=\"setColor\" />"
			+ "  <Action Name=\"toggle\" 	Method=\"toggle\" Default=\"true\" />"
			+ "  <Action Name=\"on\" 	Method=\"on\" />" + "  <Action Name=\"off\" 	Method=\"off\" />"
			+ "</HomeItem> ");

	private String lampModel = "";
	private String lampType = "";
	private String lampVersion = "";

	public DeconzLamp() {
	}

	public String getModel() {
		return MODEL;
	}

	@Override
	public boolean receiveEvent(Event event) {
		if (event.getAttribute(Event.EVENT_TYPE_ATTRIBUTE).equals("deCONZ_Message")
				&& event.getAttribute("Direction").equals("In") && event.getAttribute(DE_CONZ_ID).equals(lampId)) {
			String command = event.getAttribute("deCONZ.Command");
			if (command.equals("On")) {
				isOn = true;
			} else if (command.equals("Off")) {
				isOn = false;
			}
			String onAttribute = event.getAttribute("deCONZ.on");
			if (onAttribute != null && !onAttribute.equals("")) {
				if (onAttribute.equalsIgnoreCase("true")) {
					isOn = true;
				} else {
					isOn = false;
				}
			}
			updateAttributes(event);
			return true;
		}
		return handleInit(event);
	}

	private void updateAttributes(Event event) {
		lampModel = event.getAttribute("deCONZ.Model");
		lampType = event.getAttribute("deCONZ.Type");
		lampVersion = event.getAttribute("deCONZ.Version");
	}

	@Override
	protected boolean initAttributes(Event event) {
		lampId = event.getAttribute(DE_CONZ_ID);
//		setName(event.getAttribute("deCONZ.Name"));
		updateAttributes(event);
		return true;
	}

	protected void sendOnCommand() {
		Event ev = createEvent();
		ev.setAttribute("deCONZ.Command", "On");
		server.send(ev);
		isOn = true;
	}

	protected void sendOffCommand() {
		Event ev = createEvent();
		ev.setAttribute("deCONZ.Command", "Off");
		server.send(ev);
		isOn = false;
	}

	private Event createEvent() {
		Event ev = server.createEvent("deCONZ_Message", "");
		ev.setAttribute("Direction", "Out");
		ev.setAttribute(DE_CONZ_ID, lampId);
		return ev;
	}

	public void on() {
		sendOnCommand();
		isOn = true;
	}

	public void off() {
		sendOffCommand();
		isOn = false;
	}

	public void toggle() {
		if (isOn) {
			off();
		} else {
			on();
		}
	}

	public String getLampId() {
		return lampId;
	}

	public void setLampId(String lampId) {
		this.lampId = lampId;
	}

	public String getState() {
		return isOn ? "On" : "Off";
	}

	public boolean isOn() {
		return isOn;
	}

	public String getLampModel() {
		return lampModel;
	}

	public void setLampModel(String lampModel) {
		this.lampModel = lampModel;
	}

	public String getLampType() {
		return lampType;
	}

	public void setLampType(String lampType) {
		this.lampType = lampType;
	}

	public String getLampVersion() {
		return lampVersion;
	}

	public void setLampVersion(String lampVersion) {
		this.lampVersion = lampVersion;
	}

	private void requestLampStatusUpdate() {
		Event event = server.createEvent("ReportdeCONZLamp", "");
		event.setAttribute(DE_CONZ_ID, lampId);
		server.send(event);
	}
}