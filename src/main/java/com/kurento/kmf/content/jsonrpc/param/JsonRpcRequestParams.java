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
package com.kurento.kmf.content.jsonrpc.param;

import com.kurento.kmf.content.jsonrpc.result.JsonRpcReason;

public class JsonRpcRequestParams {
	private String sdp;
	private JsonRpcConstraints constraints;

	private JsonRpcCommand command;

	private JsonRpcReason reason; // Used in terminate

	private String sessionId;

	public static JsonRpcRequestParams newStartRequestParams(String sdp,
			JsonRpcConstraints constraints) {
		JsonRpcRequestParams params = new JsonRpcRequestParams();
		params.setSdp(sdp);
		params.setConstraints(constraints);
		return params;
	}

	public static JsonRpcRequestParams newExecuteRequestParams(
			JsonRpcCommand command, String sessionId) {
		JsonRpcRequestParams params = new JsonRpcRequestParams();
		params.setCommand(command);
		params.setSessionId(sessionId);
		return params;
	}

	public static JsonRpcRequestParams newPollRequestParams(String sessionId) {
		JsonRpcRequestParams params = new JsonRpcRequestParams();
		params.setSessionId(sessionId);
		return params;
	}

	public static JsonRpcRequestParams newTerminateRequestParams(Integer code,
			String message, String sessionId) {
		JsonRpcRequestParams params = new JsonRpcRequestParams();
		params.setReason(new JsonRpcReason(code, message));
		params.setSessionId(sessionId);
		return params;
	}

	public JsonRpcRequestParams() {
	}

	public String getSdp() {
		return sdp;
	}

	public void setSdp(String sdp) {
		this.sdp = sdp;
	}

	public JsonRpcConstraints getConstraints() {
		return constraints;
	}

	public void setConstraints(JsonRpcConstraints constraints) {
		this.constraints = constraints;
	}

	public JsonRpcCommand getCommand() {
		return command;
	}

	public void setCommand(JsonRpcCommand command) {
		this.command = command;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public JsonRpcReason getReason() {
		return reason;
	}

	public void setReason(JsonRpcReason reason) {
		this.reason = reason;
	}
}
