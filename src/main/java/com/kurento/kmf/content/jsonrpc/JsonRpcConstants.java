package com.kurento.kmf.content.jsonrpc;

/**
 * 
 * JSON-based representations for information exchange constant.
 * 
 * @see <a href="http://www.jsonrpc.org/specification">JSON error codes</a>
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class JsonRpcConstants {
	/**
	 * Start method.
	 */
	public final static String METHOD_START = "start";

	/**
	 * Terminate method.
	 */
	public final static String METHOD_TERMINATE = "terminate";

	/**
	 * Poll method.
	 */
	public final static String METHOD_POLL = "poll";

	/**
	 * Execute method.
	 */
	public final static String METHOD_EXECUTE = "execute";

	/**
	 * No error code (O).
	 */
	public final static int ERROR_NO_ERROR = 0;

	/**
	 * Application termination (-1).
	 */
	public final static int ERROR_APPLICATION_TERMINATION = 1;

	/**
	 * Invalid parameter (-32602).
	 */
	public final static int ERROR_INVALID_PARAM = -32602;

	/**
	 * Method not found (-32601).
	 */
	public final static int ERROR_METHOD_NOT_FOUND = -32601;

	/**
	 * Invalid request (-32600).
	 */
	public final static int ERROR_INVALID_REQUEST = -32600;

	/**
	 * Parser error (-32700).
	 */
	public final static int ERROR_PARSE_ERROR = -32700;

	/**
	 * Internal error (-32603).
	 */
	public final static int ERROR_INTERNAL_ERROR = -32603;

	/**
	 * Server error (32603); -32000 to -32099 are reserved to these errors.
	 */
	public final static int ERROR_SERVER_ERROR = -32000;
}
