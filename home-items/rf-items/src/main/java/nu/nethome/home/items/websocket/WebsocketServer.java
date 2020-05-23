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

package nu.nethome.home.items.websocket;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.logging.Logger;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.Gson;

import nu.nethome.home.item.AutoCreationInfo;
import nu.nethome.home.item.HomeItemAdapter;
import nu.nethome.home.item.HomeItemProxy;
import nu.nethome.home.item.HomeItemType;
import nu.nethome.home.items.deconz.JsonRestClient;
import nu.nethome.home.system.DirectoryEntry;
import nu.nethome.home.system.Event;
import nu.nethome.util.plugin.Plugin;

/**
 * Represents a deCONZ bridge and handles communications with it
 */
@SuppressWarnings("UnusedDeclaration")
@Plugin
@HomeItemType(value = "Hardware", creationInfo = WebsocketServer.DeconzCreationInfo.class)
public class WebsocketServer extends HomeItemAdapter {

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
			return String.format("Websocket server: \"%s\"", e.getAttribute("FriendlyName"));
		}
	}

	private static boolean isDeCONZUPnPEvent(Event e) {
		return e.getAttribute("ModelName").startsWith("deCONZ bridge");
	}

	private static final String MODEL = ("<?xml version = \"1.0\"?> \n"
			+ "<HomeItem Class=\"WebsocketServer\"  Category=\"Hardware\" >"
			+ "  <Attribute Name=\"State\" Type=\"String\" Get=\"getState\" Default=\"true\" />"
			+ "  <Attribute Name=\"Port\" Type=\"String\" Get=\"getPort\" Set=\"setPort\" />" + "</HomeItem> ");
	public static final String ZGP_SWITCH_TYPE = "ZGPSwitch";

	private static Logger logger = Logger.getLogger(WebsocketServer.class.getName());

	private String state = "Down";

	private static Gson gson = new Gson();

	JsonRestClient client = new JsonRestClient();
	private WebSocketServerImpl sockServer;
	private String port = "8021";

	@Override
	public String getModel() {
		return MODEL;
	}

	@Override
	public void activate() {
		state = "Running";
		try {
			int p = Integer.parseInt(port);
			sockServer = new WebSocketServerImpl(p);
			sockServer.start();
			state = "Running";
		} catch (NumberFormatException e) {
			state = "Down";
			logger.info("Port value is not an integer: " + port);
		}
	}

	@Override
	public boolean receiveEvent(Event event) {
		if (!isActivated()) {
			return handleInit(event);
		}
		List<DirectoryEntry> items = server.listInstances("");
		for (DirectoryEntry directoryEntry : items) {
			HomeItemProxy itemProxy = server.openInstance(directoryEntry.getInstanceName());
			itemProxy.getInternalRepresentation().getClass();
		}
		String eventJson = gson.toJson(event);
		sockServer.broadcast(eventJson);
//		logger.info(eventJson);
		return true;
	}

	@Override
	protected boolean initAttributes(Event event) {
		return true;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	class WebSocketServerImpl extends WebSocketServer {

		public WebSocketServerImpl(int port) {
			super(new InetSocketAddress(port));
		}

		@Override
		public void onOpen(WebSocket conn, ClientHandshake handshake) {
		}

		@Override
		public void onClose(WebSocket conn, int code, String reason, boolean remote) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onMessage(WebSocket conn, String message) {
			broadcast(message);
		}

		@Override
		public void onError(WebSocket conn, Exception ex) {
			logger.severe(ex.getMessage());
		}

		@Override
		public void onStart() {
			setConnectionLostTimeout(0);
			setConnectionLostTimeout(100);
		}

	}
}
