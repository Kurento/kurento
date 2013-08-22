package com.kurento.kmf.content.internal.jsonrpc;

import com.kurento.kmf.content.Constraints;

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

	public static JsonRpcRequest newRequest(String method, String sdp,
			String sessionId, int id, Constraints videoConstraints,
			Constraints audioConstraints) {
		return new JsonRpcRequest(method, new JsonRpcRequestParams(sdp, sessionId, videoConstraints, audioConstraints), id);
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

	public Constraints getVideoConstraints() {
		if (params != null && params.getJsonRpcConstraints() != null)
			return params.getJsonRpcConstraints().getVideoContraints();
		else
			return null;
	}

	public Constraints getAudioConstraints() {
		if (params != null && params.getJsonRpcConstraints() != null)
			return params.getJsonRpcConstraints().getAudioContraints();
		else
			return null;
	}
}

class JsonRpcRequestParams {
	private String sdp;
	private String sessionId;
	private JsonRpcConstraints constraints;

	JsonRpcRequestParams() {
	}

	JsonRpcRequestParams(String sdp, String sessionId) {
		this.sdp = sdp;
		this.sessionId = sessionId;
	}
	
	JsonRpcRequestParams(String sdp, String sessionId, Constraints videoConstraints, Constraints audioConstraints) {
		this.sdp = sdp;
		this.sessionId = sessionId;
		this.constraints = new JsonRpcConstraints(videoConstraints.toString().toLowerCase(), audioConstraints.toString().toLowerCase());
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

	JsonRpcConstraints getJsonRpcConstraints() {
		return constraints;
	}

	void setJsonRpcConstraints(JsonRpcConstraints constraints) {
		this.constraints = constraints;
	}
}

class JsonRpcConstraints {

	private String video;
	private String audio;

	public JsonRpcConstraints() {
	}

	public JsonRpcConstraints(String video, String audio) {
		this.video = video;
		this.audio = audio;
	}

	public Constraints getVideoContraints() {
		return Constraints.valueOf(getVideo().toUpperCase());
	}

	public Constraints getAudioContraints() {
		return Constraints.valueOf(getAudio().toUpperCase());
	}

	String getVideo() {
		return video;
	}

	void setVideo(String video) {
		this.video = video;
	}

	String getAudio() {
		return audio;
	}

	void setAudio(String audio) {
		this.audio = audio;
	}
}
