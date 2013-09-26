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
	private String jsonrpc;

	/**
	 * JSON RPC result.
	 */
	private JsonRpcResponseResult result;

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
		return new JsonRpcResponse(new JsonRpcResponseResult(sdp, null,
				sessionId), id);
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
		return new JsonRpcResponse(new JsonRpcResponseResult(null, url,
				sessionId), id);
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
	public static JsonRpcResponse newEventsResponse(int id,
			JsonRpcEvent... events) {
		return new JsonRpcResponse(new JsonRpcResponseResult(events), id);
	}

	public static JsonRpcResponse newCommandResponse(int id,
			String commandResult) {
		return new JsonRpcResponse(new JsonRpcResponseResult(commandResult), id);
	}

	/**
	 * Create an instance of JsonRpcResponse for acknowledge.
	 * 
	 * @param id
	 *            Response identifier
	 * @return JsonRpcResponse instance
	 */
	public static JsonRpcResponse newAckResponse(int id) {
		return new JsonRpcResponse(new JsonRpcResponseResult(), id);
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
		this.jsonrpc = "2.0";
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
		this.jsonrpc = "2.0";
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

	/**
	 * SDP message accessor (getter).
	 * 
	 * @return SDP message
	 */
	public String getSdp() {
		if (result == null)
			return null;
		else
			return result.getSdp();
	}

	public String getCommandResult() {
		if (result == null)
			return null;
		else
			return result.getCommandResult();
	}

	/**
	 * Session identifier accessor (getter).
	 * 
	 * @return Session identifier
	 */
	public String getSessionId() {
		if (result == null)
			return null;
		else
			return result.getSessionId();
	}

	/**
	 * Error code accessor (getter).
	 * 
	 * @return Error code
	 */
	public int getErrorCode() {
		if (error == null)
			return 0;
		else
			return error.getCode();
	}

	/**
	 * Error message accessor (getter).
	 * 
	 * @return Error message
	 */
	public String gerErrorMessage() {
		if (error == null)
			return null;
		else
			return error.getMessage();
	}

	/**
	 * Error data accessor (getter).
	 * 
	 * @return Error data
	 */
	public String getErrorData() {
		if (error == null)
			return null;
		else
			return error.getData();
	}

	/**
	 * JSON RPC event accessor (getter).
	 * 
	 * @return JSON RPC event
	 */
	public JsonRpcEvent[] getEvents() {
		if (result == null) {
			return null;
		} else {
			return result.getJsonRpcEvents();
		}
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

/**
 * 
 * JSON RPC response result Java representation.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
class JsonRpcResponseResult {

	/**
	 * SDP message.
	 */
	private String sdp;

	/**
	 * Media URL.
	 */
	private String url;

	/**
	 * Session identifier.
	 */
	private String sessionId;

	private String commandResult;

	/**
	 * JSON RPC events array.
	 */
	private JsonRpcEvent[] events;

	/**
	 * Default constructor.
	 */
	JsonRpcResponseResult() {
	}

	/**
	 * Parameterized constructor.
	 * 
	 * @param sdp
	 *            SDP message
	 * @param url
	 *            Media URL
	 * @param sessionId
	 *            Session identifier
	 */
	JsonRpcResponseResult(String sdp, String url, String sessionId) {
		this.sdp = sdp;
		this.url = url;
		this.sessionId = sessionId;
	}

	JsonRpcResponseResult(String commandResult) {
		this.commandResult = commandResult;
	}

	/**
	 * Parameterized (by events) constructor.
	 * 
	 * @param events
	 *            JSON RPC events array
	 */
	JsonRpcResponseResult(JsonRpcEvent[] events) {
		this.events = events;
	}

	/**
	 * SDP message accessor (getter).
	 * 
	 * @return SDP message
	 */
	String getSdp() {
		return sdp;
	}

	/**
	 * SDP message mutator (setter).
	 * 
	 * @param sdp
	 *            SDP message
	 */
	void setSdp(String sdp) {
		this.sdp = sdp;
	}

	/**
	 * Media URL accessor (getter).
	 * 
	 * @return Media URL
	 */
	String getUrl() {
		return url;
	}

	/**
	 * Media URL mutator (setter).
	 * 
	 * @param url
	 *            Media URL
	 */
	void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Session identifier accessor (getter).
	 * 
	 * @return Sesion identifier
	 */
	String getSessionId() {
		return sessionId;
	}

	/**
	 * Session identifier mutator (getter).
	 * 
	 * @param sessionId
	 *            Session identifier
	 */
	void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * JSON RPC events accessor (getter).
	 * 
	 * @return JSON RPC events array
	 */
	JsonRpcEvent[] getJsonRpcEvents() {
		return events;
	}

	public String getCommandResult() {
		return commandResult;
	}

	public void setCommandResult(String commandResult) {
		this.commandResult = commandResult;
	}
}

/**
 * 
 * JSON RPC response error Java representation.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
class JsonRpcResponseError {

	/**
	 * Error status code.
	 */
	private int code;

	/**
	 * Error message.
	 */
	private String message;

	/**
	 * Error data.
	 */
	private String data;

	/**
	 * Default constructor.
	 */
	JsonRpcResponseError() {
	}

	/**
	 * Parameterized cosntructor.
	 * 
	 * @param code
	 *            Error status code
	 * @param message
	 *            Error message
	 * @param data
	 *            Error data
	 */
	JsonRpcResponseError(int code, String message, String data) {
		this.code = code;
		this.message = message;
		this.data = data;
	}

	/**
	 * Error status code accessor (getter).
	 * 
	 * @return Error status code
	 */
	int getCode() {
		return code;
	}

	/**
	 * Error status code mutator (setter).
	 * 
	 * @param code
	 *            Error status code
	 */
	void setCode(int code) {
		this.code = code;
	}

	/**
	 * Error message accessor (getter).
	 * 
	 * @return Error message
	 */
	String getMessage() {
		return message;
	}

	/**
	 * Error message mutator (setter).
	 * 
	 * @param message
	 *            Error message
	 */
	void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Error data accessor (getter).
	 * 
	 * @return Error data
	 */
	String getData() {
		return data;
	}

	/**
	 * Error data mutator (setter).
	 * 
	 * @param data
	 *            Error data
	 */
	void setData(String data) {
		this.data = data;
	}
}
