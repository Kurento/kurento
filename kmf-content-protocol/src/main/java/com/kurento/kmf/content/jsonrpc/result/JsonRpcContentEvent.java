/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.kmf.content.jsonrpc.result;

/**
 * 
 * Java representation for JSON events.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class JsonRpcContentEvent {

	/**
	 * Event type.
	 */
	private String type;

	/**
	 * Event data.
	 */
	private String data;

	/**
	 * Static instance of JsonRpcEvent.
	 * 
	 * @param type
	 *            Event type
	 * @param data
	 *            Event data
	 * @return JsonRpcEvent instance
	 */
	public static JsonRpcContentEvent newEvent(String type, String data) {
		return new JsonRpcContentEvent(type, data);
	}

	/**
	 * Default constructor.
	 */
	public JsonRpcContentEvent() {
	}

	/**
	 * Parameterized constructor.
	 * 
	 * @param type
	 *            Event type
	 * @param data
	 *            Event data
	 */
	public JsonRpcContentEvent(String type, String data) {
		this.type = type;
		this.data = data;
	}

	/**
	 * Type accessor (getter).
	 * 
	 * @return event type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Data accessor (getter).
	 * 
	 * @return event data
	 */
	public String getData() {
		return data;
	}

}
