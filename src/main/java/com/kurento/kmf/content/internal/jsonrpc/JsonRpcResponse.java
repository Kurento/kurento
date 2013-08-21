package com.kurento.kmf.content.internal.jsonrpc;

public class JsonRpcResponse {
	private String jsonrpc;
	private JsonRpcResponseResult result;
	private JsonRpcResponseError error;
	private int id;

	public static JsonRpcResponse newStartSdpResponse(String sdp,
			String sessionId, int id) {
		return new JsonRpcResponse(new JsonRpcResponseResult(sdp, null,
				sessionId), id);
	}

	public static JsonRpcResponse newStartUrlResponse(String url,
			String sessionId, int id) {
		return new JsonRpcResponse(new JsonRpcResponseResult(null, url,
				sessionId), id);
	}

	public static JsonRpcResponse newEventsResponse(int id,
			JsonRpcEvent... events) {
		return new JsonRpcResponse(new JsonRpcResponseResult(events), id);
	}

	public static JsonRpcResponse newAckResponse(int id) {
		return new JsonRpcResponse(new JsonRpcResponseResult(), id);
	}

	public static JsonRpcResponse newError(int code, String message, int id) {
		return new JsonRpcResponse(
				new JsonRpcResponseError(code, message, null), id);
	}

	public static JsonRpcResponse newError(int code, String message,
			String data, int id) {
		return new JsonRpcResponse(
				new JsonRpcResponseError(code, message, data), id);
	}

	JsonRpcResponse() {
	}

	JsonRpcResponse(JsonRpcResponseResult result, int id) {
		this.jsonrpc = "2.0";
		this.result = result;
		this.id = id;
	}

	JsonRpcResponse(JsonRpcResponseError error, int id) {
		this.jsonrpc = "2.0";
		this.error = error;
		this.id = id;
	}

	public String getJsonRpcVersion() {
		return jsonrpc;
	}

	public boolean isError() {
		return error != null;
	}

	public String getSdp() {
		if (result == null)
			return null;
		else
			return result.getSdp();
	}

	public String getSessionId() {
		if (result == null)
			return null;
		else
			return result.getSessionId();
	}

	public int getErrorCode() {
		if (error == null)
			return 0;
		else
			return error.getCode();
	}

	public String gerErrorMessage() {
		if (error == null)
			return null;
		else
			return error.getMessage();
	}

	public String getErrorData() {
		if (error == null)
			return null;
		else
			return error.getData();
	}

	public JsonRpcEvent[] getEvents() {
		if (result == null) {
			return null;
		} else {
			return result.getJsonRpcEvents();
		}
	}

	public int getId() {
		return id;
	}
}

class JsonRpcResponseResult {
	private String sdp;
	private String url;
	private String sessionId;
	private JsonRpcEvent[] events;

	JsonRpcResponseResult() {
	}

	JsonRpcResponseResult(String sdp, String url, String sessionId) {
		this.sdp = sdp;
		this.url = url;
		this.sessionId = sessionId;
	}

	JsonRpcResponseResult(JsonRpcEvent[] events) {
		this.events = events;
	}

	String getSdp() {
		return sdp;
	}

	void setSdp(String sdp) {
		this.sdp = sdp;
	}

	String getUrl() {
		return url;
	}

	void setUrl(String url) {
		this.url = url;
	}

	String getSessionId() {
		return sessionId;
	}

	void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	JsonRpcEvent[] getJsonRpcEvents() {
		return events;
	}
}

class JsonRpcResponseError {
	private int code;
	private String message;
	private String data;

	JsonRpcResponseError() {
	}

	JsonRpcResponseError(int code, String message, String data) {
		this.code = code;
		this.message = message;
		this.data = data;
	}

	int getCode() {
		return code;
	}

	void setCode(int code) {
		this.code = code;
	}

	String getMessage() {
		return message;
	}

	void setMessage(String message) {
		this.message = message;
	}

	String getData() {
		return data;
	}

	void setData(String data) {
		this.data = data;
	}
}
