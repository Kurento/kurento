package com.kurento.kmf.content.internal.jsonrpc;

public class WebRtcJsonRequest {
	private String jsonrpc;
	private String method;
	private WebRtcJsonRequestParams params;
	private int id;

	public static WebRtcJsonRequest newRequest(String method, String sdp,
			String sessionId, int id) {
		return new WebRtcJsonRequest(method, new WebRtcJsonRequestParams(sdp,
				sessionId), id);
	}

	WebRtcJsonRequest() {
	}

	WebRtcJsonRequest(String method, WebRtcJsonRequestParams params, int id) {
		this.jsonrpc = "2.0";
		this.method = method;
		this.params = params;
		this.id = id;
	}

	public String getSdp() {
		if (params != null)
			return params.getSdp();
		else
			return null;
	}

	public String getSessionId() {
		if (params != null)
			return params.getSessionId();
		else
			return null;
	}

	public String getJsonRpcVersion() {
		return jsonrpc;
	}

	public String getMethod() {
		return method;
	}

	public int getId() {
		return id;
	}

}

class WebRtcJsonRequestParams {
	private String sdp;
	private String sessionId;

	WebRtcJsonRequestParams() {
	}

	WebRtcJsonRequestParams(String sdp, String sessionId) {
		this.sdp = sdp;
		this.sessionId = sessionId;
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
}
