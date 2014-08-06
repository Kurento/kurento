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
package org.kurento.jsonrpcconnector.internal.message;

import com.google.gson.annotations.SerializedName;

import org.kurento.jsonrpcconnector.JsonUtils;
import org.kurento.jsonrpcconnector.internal.JsonRpcConstants;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 * 
 */
public abstract class Message {

	/**
	 * JSON RPC version.
	 */
	@SerializedName("jsonrpc")
	private final String jsonrpc = JsonRpcConstants.JSON_RPC_VERSION;

	protected transient String sessionId;

	public Message() {
	}

	public Message(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getVersion() {
		return jsonrpc;
	}

	@Override
	public String toString() {
		return JsonUtils.toJsonMessage(this);
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

}
