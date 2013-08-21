package com.kurento.kmf.content.internal.jsonrpc;

public class JsonRpcRequest {
	private String jsonrpc;
	private String method;
	private JsonRpcRequestParams params;
	private int id;

	public static JsonRpcRequest newRequest(String method, String sdp,
			String sessionId, int id) {
		return new JsonRpcRequest(method, new JsonRpcRequestParams(sdp,
				sessionId), id);
	}

	JsonRpcRequest() {
	}

	JsonRpcRequest(String method, JsonRpcRequestParams params, int id) {
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

class JsonRpcRequestParams {
	private String sdp;
	private String sessionId;

	JsonRpcRequestParams() {
	}

	JsonRpcRequestParams(String sdp, String sessionId) {
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
