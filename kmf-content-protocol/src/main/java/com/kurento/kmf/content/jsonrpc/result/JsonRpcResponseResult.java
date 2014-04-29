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
 * JSON RPC response result Java representation.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class JsonRpcResponseResult {

	// Fields belonging to start responses
	private JsonRpcReason rejected;
	private String sdp;
	private String url;
	private String sessionId;

	// Fields belonging to execute responses
	private String commandResult;

	// Field belonging to poll responses
	private JsonRpcContentEvent[] contentEvents;
	private JsonRpcControlEvent[] controlEvents;

	// Terminate responses are just simple ACKs (empty result)

	public static JsonRpcResponseResult newStartSdpResponseResult(String sdp,
			String sessionId) {
		JsonRpcResponseResult result = new JsonRpcResponseResult();
		result.setSdp(sdp);
		result.setSessionId(sessionId);
		return result;
	}

	public static JsonRpcResponseResult newStartUrlResponseResult(String url,
			String sessionId) {
		JsonRpcResponseResult result = new JsonRpcResponseResult();
		result.setUrl(url);
		result.setSessionId(sessionId);
		return result;
	}

	public static JsonRpcResponseResult newStartRejectResponseResult(int code,
			String message) {
		JsonRpcResponseResult result = new JsonRpcResponseResult();
		result.setRejected(new JsonRpcReason(code, message));
		return result;
	}

	public static JsonRpcResponseResult newExecuteResponseResult(
			String commandResult) {
		JsonRpcResponseResult result = new JsonRpcResponseResult();
		result.setCommandResult(commandResult);
		return result;
	}

	public static JsonRpcResponseResult newExecuteResponseResult(
			String sessionId, String commandResult) {
		JsonRpcResponseResult result = new JsonRpcResponseResult();
		result.setCommandResult(commandResult);
		result.setSessionId(sessionId);
		return result;
	}

	public static JsonRpcResponseResult newPollResponseResult(
			JsonRpcContentEvent[] contentEvents,
			JsonRpcControlEvent[] controlEvents) {
		JsonRpcResponseResult result = new JsonRpcResponseResult();
		result.setContentEvents(contentEvents);
		result.setControlEvents(controlEvents);
		return result;
	}

	public static JsonRpcResponseResult newTerminateResponseResult() {
		JsonRpcResponseResult result = new JsonRpcResponseResult();
		return result;
	}

	public JsonRpcResponseResult() {
	}

	public JsonRpcReason getRejected() {
		return rejected;
	}

	public void setRejected(JsonRpcReason rejected) {
		this.rejected = rejected;
	}

	public String getSdp() {
		return sdp;
	}

	public void setSdp(String sdp) {
		this.sdp = sdp;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getCommandResult() {
		return commandResult;
	}

	public void setCommandResult(String commandResult) {
		this.commandResult = commandResult;
	}

	public JsonRpcContentEvent[] getContentEvents() {
		return contentEvents;
	}

	public void setContentEvents(JsonRpcContentEvent[] contentEvents) {
		this.contentEvents = contentEvents;
	}

	public JsonRpcControlEvent[] getControlEvents() {
		return controlEvents;
	}

	public void setControlEvents(JsonRpcControlEvent[] controlEvents) {
		this.controlEvents = controlEvents;
	}

}
