package com.kurento.kmf.content.jsonrpc;

/**
 * 
 * Java representation for JSON request.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class JsonRpcRequest {

	/**
	 * JSON RPC version.
	 */
	private String jsonrpc;

	/**
	 * JSPON RPC method.
	 */
	private String method;

	/**
	 * JSON RPC request parameters.
	 */
	private JsonRpcRequestParams params;

	/**
	 * Request identifier.
	 */
	private int id;

	/**
	 * Create an instance of JsonRpcRequest.
	 * 
	 * @param method
	 *            JSPON RPC method
	 * @param sdp
	 *            Received SDP
	 * @param sessionId
	 *            Session identifier
	 * @param id
	 *            Request identifier
	 * @return JsonRpcRequest instance
	 */
	public static JsonRpcRequest newRequest(String method, String sdp,
			String sessionId, int id) {
		return new JsonRpcRequest(method, new JsonRpcRequestParams(sdp,
				sessionId), id);
	}

	/**
	 * Create an instance of JsonRpcRequest with video/audio constraints.
	 * 
	 * @param method
	 * @param sdp
	 * @param sessionId
	 * @param id
	 * @param videoConstraints
	 * @param audioConstraints
	 * @return
	 */
	public static JsonRpcRequest newRequest(String method, String sdp,
			String sessionId, int id, Constraints videoConstraints,
			Constraints audioConstraints) {
		return new JsonRpcRequest(method, new JsonRpcRequestParams(sdp,
				sessionId, videoConstraints, audioConstraints), id);
	}

	/**
	 * TODO
	 * 
	 * @param method
	 * @param commandType
	 * @param commandData
	 * @param id
	 * @return
	 */
	//TODO: for symmetry rename rest of newRequest methods?
	public static JsonRpcRequest newCommandRequest(String method, String commandType,
			String commandData, int id) {
		return new JsonRpcRequest(method, new JsonRpcRequestParams(
				new JsonRpcCommand(commandType, commandData)), id);
	}

	/**
	 * Default constructor.
	 */
	JsonRpcRequest() {
	}

	/**
	 * Parameterized constructor.
	 * 
	 * @param method
	 *            JSPON RPC method
	 * @param params
	 *            JSON parameters
	 * @param id
	 *            Request identifier
	 */
	JsonRpcRequest(String method, JsonRpcRequestParams params, int id) {
		this.jsonrpc = "2.0";
		this.method = method;
		this.params = params;
		this.id = id;
	}

	/**
	 * SDP accessor (getter).
	 * 
	 * @return received SDP
	 */
	public String getSdp() {
		if (params != null)
			return params.getSdp();
		else
			return null;
	}

	public String getCommandType() {
		if (params != null && params.getCommand() != null)
			return params.getCommand().getType();
		else
			return null;
	}

	public String getCommandData() {
		if (params != null && params.getCommand() != null)
			return params.getCommand().getData();
		else
			return null;
	}

	/**
	 * Session identifier accessor (getter).
	 * 
	 * @return Session identifier
	 */
	public String getSessionId() {
		if (params != null)
			return params.getSessionId();
		else
			return null;
	}

	/**
	 * JSON RPC version accessor.
	 * 
	 * @return JSON RPC version
	 */
	public String getJsonRpcVersion() {
		return jsonrpc;
	}

	/**
	 * JSON Method accessor (getter).
	 * 
	 * @return JSON Method
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * Request identifier accessor.
	 * 
	 * @return Request identifier
	 */
	public int getId() {
		return id;
	}

	/**
	 * Video constraints accessor (getter).
	 * 
	 * @return Video constraints
	 */
	public Constraints getVideoConstraints() {
		if (params != null && params.getJsonRpcConstraints() != null)
			return params.getJsonRpcConstraints().getVideoContraints();
		else
			return null;
	}

	/**
	 * Audio constraints accessor (getter).
	 * 
	 * @return Audio constraints
	 */
	public Constraints getAudioConstraints() {
		if (params != null && params.getJsonRpcConstraints() != null)
			return params.getJsonRpcConstraints().getAudioContraints();
		else
			return null;
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
 * Java representation for JSON parameters.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
class JsonRpcRequestParams {
	/**
	 * SDP message.
	 */
	private String sdp;

	/**
	 * Session identifier.
	 */
	private String sessionId;

	private JsonRpcCommand command;

	/**
	 * JSON RPC constraints.
	 */
	private JsonRpcConstraints constraints;

	/**
	 * Default constructor.
	 */
	JsonRpcRequestParams() {
	}

	/**
	 * Parameterized constructor.
	 * 
	 * @param sdp
	 *            SDP message
	 * @param sessionId
	 *            Session identifier
	 */
	JsonRpcRequestParams(String sdp, String sessionId) {
		this.sdp = sdp;
		this.sessionId = sessionId;
	}

	/**
	 * Parameterized constructor with video/audio constraints.
	 * 
	 * @param sdp
	 *            SDP message
	 * @param sessionId
	 *            Session identifier
	 * @param videoConstraints
	 *            Audio constraints
	 * @param audioConstraints
	 *            Video constraints
	 */
	JsonRpcRequestParams(String sdp, String sessionId,
			Constraints videoConstraints, Constraints audioConstraints) {
		this.sdp = sdp;
		this.sessionId = sessionId;
		this.constraints = new JsonRpcConstraints(videoConstraints.toString()
				.toLowerCase(), audioConstraints.toString().toLowerCase());
	}

	/**
	 * Parameterized constructor with video/audio constraints.
	 * 
	 * TODO
	 */
	JsonRpcRequestParams(JsonRpcCommand command) {
		this.command = command;
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
	 * Session identifier accessor (getter).
	 * 
	 * @return Session identifier.
	 */
	String getSessionId() {
		return sessionId;
	}

	/**
	 * Session identifier mutator (setter).
	 * 
	 * @param sessionId
	 *            Session identifier
	 */
	void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * JSON RPC constraints accessor (getter).
	 * 
	 * @return JSON RPC constraints
	 */
	JsonRpcConstraints getJsonRpcConstraints() {
		return constraints;
	}

	/**
	 * JSON RPC constraints mutator (setter).
	 * 
	 * @param constraints
	 *            JSON RPC constraints
	 */
	void setJsonRpcConstraints(JsonRpcConstraints constraints) {
		this.constraints = constraints;
	}

	public JsonRpcCommand getCommand() {
		return command;
	}

	public void setCommand(JsonRpcCommand command) {
		this.command = command;
	}

}

/**
 * 
 * Java representation for JSON commands.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
class JsonRpcCommand {
	private String type;
	private String data;

	public JsonRpcCommand() {
	}

	public JsonRpcCommand(String type, String data) {
		this.type = type;
		this.data = data;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}

/**
 * 
 * Java representation for JSON constraints.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
class JsonRpcConstraints {

	/**
	 * Audio constraints.
	 */
	private String video;

	/**
	 * Video constraints.
	 */
	private String audio;

	/**
	 * Default constructor.
	 */
	public JsonRpcConstraints() {
	}

	/**
	 * Parameterized constructor.
	 * 
	 * @param video
	 *            Audio constraints
	 * @param audio
	 *            Video constraints
	 */
	public JsonRpcConstraints(String video, String audio) {
		this.video = video;
		this.audio = audio;
	}

	/**
	 * Video constraints accessor (getter), returned as upper case.
	 * 
	 * @return Upper case video constraints
	 */
	public Constraints getVideoContraints() {
		return Constraints.valueOf(getVideo().toUpperCase());
	}

	/**
	 * Audio constraints accessor (getter), returned as upper case.
	 * 
	 * @return Upper case audio constraints
	 */
	public Constraints getAudioContraints() {
		return Constraints.valueOf(getAudio().toUpperCase());
	}

	/**
	 * Video constraints accessor (getter).
	 * 
	 * @return Video constraints
	 */
	String getVideo() {
		return video;
	}

	/**
	 * Video constraints mutator (setter).
	 * 
	 * @param video
	 *            Video constraints
	 */
	void setVideo(String video) {
		this.video = video;
	}

	/**
	 * Audio constraints accessor (getter).
	 * 
	 * @return Audio constraints
	 */
	String getAudio() {
		return audio;
	}

	/**
	 * Audio constraints mutator (setter).
	 * 
	 * @param audio
	 *            Audio constraints
	 */
	void setAudio(String audio) {
		this.audio = audio;
	}
}
