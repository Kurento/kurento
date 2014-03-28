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
package com.kurento.kmf.content.jsonrpc;

import com.kurento.kmf.content.jsonrpc.result.JsonRpcContentEvent;
import com.kurento.kmf.content.jsonrpc.result.JsonRpcControlEvent;
import com.kurento.kmf.content.jsonrpc.result.JsonRpcResponseResult;

/**
 * 
 * Java representation for JSON response.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class JsonRpcResponse {

	/**
	 * JSON RPC version.
	 */
	private String jsonrpc = JsonRpcConstants.JSON_RPC_VERSION;

	/**
	 * JSON RPC result.
	 */
	JsonRpcResponseResult result;

	/**
	 * JSON RPC error.
	 */
	private JsonRpcResponseError error;

	/**
	 * Response identifier.
	 */
	private int id;

	/**
	 * Create an instance of JsonRpcResponse from an SDP message.
	 * 
	 * @param sdp
	 *            SDP message
	 * @param sessionId
	 *            Session identifier
	 * @param id
	 *            Response identifier
	 * @return JsonRpcResponse instance
	 */
	public static JsonRpcResponse newStartSdpResponse(String sdp,
			String sessionId, int id) {
		return new JsonRpcResponse(
				JsonRpcResponseResult.newStartSdpResponseResult(sdp, sessionId),
				id);
	}

	/**
	 * Create an instance of JsonRpcResponse from an URL.
	 * 
	 * @param url
	 *            Media URL
	 * @param sessionId
	 *            Session identifier
	 * @param id
	 *            Response identifier
	 * @return JsonRpcResponse instance
	 */
	public static JsonRpcResponse newStartUrlResponse(String url,
			String sessionId, int id) {
		return new JsonRpcResponse(
				JsonRpcResponseResult.newStartUrlResponseResult(url, sessionId),
				id);
	}

	public static JsonRpcResponse newStartRejectedResponse(int code,
			String message, int id) {
		return new JsonRpcResponse(
				JsonRpcResponseResult.newStartRejectResponseResult(code,
						message), id);
	}

	/**
	 * Create an instance of JsonRpcResponse from a list of events.
	 * 
	 * @param id
	 *            Response identifier
	 * @param events
	 *            List of JSON RPC events
	 * @return JsonRpcResponse instance
	 */
	public static JsonRpcResponse newPollResponse(
			JsonRpcContentEvent[] contentEvents,
			JsonRpcControlEvent[] controlEvents, int id) {
		return new JsonRpcResponse(JsonRpcResponseResult.newPollResponseResult(
				contentEvents, controlEvents), id);
	}

	public static JsonRpcResponse newExecuteResponse(String commandResult,
			int id) {
		return new JsonRpcResponse(
				JsonRpcResponseResult.newExecuteResponseResult(commandResult),
				id);
	}

	public static JsonRpcResponse newExecuteResponse(String sessionId,
			String commandResult, int id) {
		return new JsonRpcResponse(
				JsonRpcResponseResult.newExecuteResponseResult(sessionId,
						commandResult), id);
	}

	/**
	 * Create an instance of JsonRpcResponse for acknowledge.
	 * 
	 * @param id
	 *            Response identifier
	 * @return JsonRpcResponse instance
	 */
	public static JsonRpcResponse newTerminateResponse(int code,
			String message, int id) {
		return new JsonRpcResponse(
				JsonRpcResponseResult.newTerminateResponseResult(), id);
	}

	/**
	 * Create an instance of JsonRpcResponse for error without data.
	 * 
	 * @param code
	 *            Error code
	 * @param message
	 *            Error message
	 * @param id
	 *            Response identifier
	 * @return JsonRpcResponse instance
	 */
	public static JsonRpcResponse newError(int code, String message, int id) {
		return new JsonRpcResponse(
				new JsonRpcResponseError(code, message, null), id);
	}

	/**
	 * Create an instance of JsonRpcResponse for error with data.
	 * 
	 * @param code
	 *            Error code
	 * @param message
	 *            Error message
	 * @param data
	 *            JSON RPC data
	 * @param id
	 *            Response identifier
	 * @return JsonRpcResponse instance
	 */
	public static JsonRpcResponse newError(int code, String message,
			String data, int id) {
		return new JsonRpcResponse(
				new JsonRpcResponseError(code, message, data), id);
	}

	/**
	 * Default constructor.
	 */
	JsonRpcResponse() {

	}

	/**
	 * Parameterized constructor; default JSON RPC version 2.0.
	 * 
	 * @param result
	 *            JSON RPC result
	 * @param id
	 *            Response identifier
	 */
	JsonRpcResponse(JsonRpcResponseResult result, int id) {
		this.result = result;
		this.id = id;
	}

	/**
	 * Parameterized constructor; default JSON RPC version 2.0.
	 * 
	 * @param error
	 *            JSON RPC error
	 * @param id
	 *            Response identifier
	 */
	JsonRpcResponse(JsonRpcResponseError error, int id) {
		this.error = error;
		this.id = id;
	}

	/**
	 * JSON RPC version accessor (getter).
	 * 
	 * @return JSON RPC version
	 */
	public String getJsonRpcVersion() {
		return jsonrpc;
	}

	/**
	 * JSON RPC error accessor (getter).
	 * 
	 * @return JSON RPC error
	 */
	public boolean isError() {
		return error != null;
	}

	public JsonRpcResponseResult getResponseResult() {
		return result;
	}

	public JsonRpcResponseError getResponseError() {
		return error;
	}

	/**
	 * Response identifier accessor (getter).
	 * 
	 * @return Response identifier
	 */
	public int getId() {
		return id;
	}

	/**
	 * Parses Java class to JSON.
	 */
	@Override
	public String toString() {
		return GsonUtils.toString(this);
	}
}
