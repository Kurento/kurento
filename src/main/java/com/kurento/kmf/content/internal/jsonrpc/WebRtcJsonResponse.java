package com.kurento.kmf.content.internal.jsonrpc;

public class WebRtcJsonResponse {
	private String jsonrpc;
	private WebRtcJsonResponseResult result;
	private WebRtcJsonResponseError error;
	private int id;

	public static WebRtcJsonResponse newStartResponse(String sdp,
			String sessionId, int id) {
		return new WebRtcJsonResponse(new WebRtcJsonResponseResult(sdp,
				sessionId), id);
	}

	public static WebRtcJsonResponse newEventsResponse(int id,
			WebRtcJsonEvent... events) {
		return new WebRtcJsonResponse(new WebRtcJsonResponseResult(events), id);
	}

	public static WebRtcJsonResponse newAckResponse(int id) {
		return new WebRtcJsonResponse(new WebRtcJsonResponseResult(), id);
	}

	public static WebRtcJsonResponse newError(int code, String message, int id) {
		return new WebRtcJsonResponse(new WebRtcJsonResponseError(code,
				message, null), id);
	}

	public static WebRtcJsonResponse newError(int code, String message,
			String data, int id) {
		return new WebRtcJsonResponse(new WebRtcJsonResponseError(code,
				message, data), id);
	}

	WebRtcJsonResponse() {
	}

	WebRtcJsonResponse(WebRtcJsonResponseResult result, int id) {
		this.jsonrpc = "2.0";
		this.result = result;
		this.id = id;
	}

	WebRtcJsonResponse(WebRtcJsonResponseError error, int id) {
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

	public WebRtcJsonEvent[] getEvents() {
		if (result == null) {
			return null;
		} else {
			return result.getWebRtcJsonEvents();
		}
	}

	public int getId() {
		return id;
	}
}

class WebRtcJsonResponseResult {
	private String sdp;
	private String sessionId;
	private WebRtcJsonEvent[] events;

	WebRtcJsonResponseResult() {
	}

	WebRtcJsonResponseResult(String sdp, String sessionId) {
		this.sdp = sdp;
		this.sessionId = sessionId;
	}

	WebRtcJsonResponseResult(WebRtcJsonEvent[] events) {
		this.events = events;
	}

	String getSdp() {
		return sdp;
	}

	void setSdp(String sdp) {
		this.sdp = sdp;
	}

	String getSessionId() {
		return sessionId;
	}

	void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	WebRtcJsonEvent[] getWebRtcJsonEvents() {
		return events;
	}
}

class WebRtcJsonResponseError {
	private int code;
	private String message;
	private String data;

	WebRtcJsonResponseError() {
	}

	WebRtcJsonResponseError(int code, String message, String data) {
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
