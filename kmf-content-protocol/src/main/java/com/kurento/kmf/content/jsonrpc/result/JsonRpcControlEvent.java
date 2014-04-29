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

public class JsonRpcControlEvent {

	private String type;
	private JsonRpcReason data;

	public static JsonRpcControlEvent newEvent(String type, Integer code,
			String message) {
		return new JsonRpcControlEvent(type, new JsonRpcReason(code, message));
	}

	/**
	 * Default constructor.
	 */
	public JsonRpcControlEvent() {
	}

	/**
	 * Parameterized constructor.
	 * 
	 * @param type
	 *            Event type
	 * @param data
	 *            Event data
	 */
	public JsonRpcControlEvent(String type, JsonRpcReason data) {
		this.type = type;
		this.setData(data);
	}

	/**
	 * Type accessor (getter).
	 * 
	 * @return event type
	 */
	public String getType() {
		return type;
	}

	public JsonRpcReason getData() {
		return data;
	}

	public void setData(JsonRpcReason data) {
		this.data = data;
	}
}
