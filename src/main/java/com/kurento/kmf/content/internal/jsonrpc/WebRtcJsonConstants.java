package com.kurento.kmf.content.internal.jsonrpc;

public class WebRtcJsonConstants {
	public final static String METHOD_START = "start";
	public final static String METHOD_TERMINATE = "terminate";
	public final static String METHOD_POLL = "poll";

	//Error codes from http://www.jsonrpc.org/specification
	public final static int ERROR_NO_ERROR = 0;
	public final static int ERROR_APPLICATION_TERMINATION = 1;
	public final static int ERROR_INVALID_PARAM = -32602;
	public final static int ERROR_METHOD_NOT_FOUND = -32601;
	public final static int ERROR_INVALID_REQUEST =- 32600;
	public final static int ERROR_PARSE_ERROR = -32700;
	public final static int ERROR_INTERNAL_ERROR = -32603;
	//-32000 to -32099 	reserved to Server error
	public final static int ERROR_SERVER_ERROR = -32000;
}
