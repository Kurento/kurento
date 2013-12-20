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
package com.kurento.kmf.jsonrpcconnector.internal;

/**
 * 
 * JSON-based representations for information exchange constant.
 * 
 * @see <a href="http://www.jsonrpc.org/specification">JSON error codes</a>
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class JsonRpcConstants {

	public final static String JSON_RPC_VERSION = "2.0";

	public final static String JSON_RPC_PROPERTY = "jsonrpc";

	public final static String PARAMS_PROPERTY = "params";

	public final static String ID_PROPERTY = "id";

	public final static String RESULT_PROPERTY = "result";

	public static final String ERROR_PROPERTY = "error";

	public static final String DATA_PROPERTY = "data";

	public final static String METHOD_PROPERTY = "method";

	public final static String SESSION_ID_PROPERTY = "sessionId";

	public final static String METHOD_START = "start";

	public final static String METHOD_TERMINATE = "terminate";

	public final static String METHOD_POLL = "poll";

	public final static String METHOD_EXECUTE = "execute";

	public static final String METHOD_RECONNECT = "reconnect";

	public final static String EVENT_SESSION_TERMINATED = "sessionTerminated";

	public final static String EVENT_SESSION_ERROR = "sessionError";

	public static final String RECONNECTION_ERROR = "reconnection error";

	public static final String RECONNECTION_SUCCESSFUL = "reconnection successful";

	public final static int ERROR_NO_ERROR = 0;

	public final static int ERROR_APPLICATION_TERMINATION = 1;

	public final static int ERROR_INVALID_PARAM = -32602;

	public final static int ERROR_METHOD_NOT_FOUND = -32601;

	public final static int ERROR_INVALID_REQUEST = -32600;

	public final static int ERROR_PARSE_ERROR = -32700;

	public final static int ERROR_INTERNAL_ERROR = -32603;

	public final static int ERROR_SERVER_ERROR = -32000;

}
