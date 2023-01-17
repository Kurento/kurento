/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.jsonrpc.internal;

/**
 *
 * JSON-based representations for information exchange constant.
 *
 * @see <a href="http://www.jsonrpc.org/specification">JSON error codes</a>
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class JsonRpcConstants {

  public static final String JSON_RPC_VERSION = "2.0";

  public static final String JSON_RPC_PROPERTY = "jsonrpc";

  public static final String PARAMS_PROPERTY = "params";

  public static final String ID_PROPERTY = "id";

  public static final String RESULT_PROPERTY = "result";

  public static final String ERROR_PROPERTY = "error";

  public static final String DATA_PROPERTY = "data";

  public static final String METHOD_PROPERTY = "method";

  public static final String SESSION_ID_PROPERTY = "sessionId";

  public static final String METHOD_START = "start";

  public static final String METHOD_TERMINATE = "terminate";

  public static final String METHOD_POLL = "poll";

  public static final String METHOD_EXECUTE = "execute";

  public static final String METHOD_CONNECT = "connect";

  public static final String METHOD_PING = "ping";

  public static final String METHOD_CLOSE = "closeSession";

  public static final String PONG_PAYLOAD = "value";

  public static final String PONG = "pong";

  public static final String EVENT_SESSION_TERMINATED = "sessionTerminated";

  public static final String EVENT_SESSION_ERROR = "sessionError";

  public static final String RECONNECTION_ERROR = "reconnection error";

  public static final String RECONNECTION_SUCCESSFUL = "reconnection successful";

  public static final int ERROR_NO_ERROR = 0;

  public static final int ERROR_APPLICATION_TERMINATION = 1;

  public static final int ERROR_INVALID_PARAM = -32602;

  public static final int ERROR_METHOD_NOT_FOUND = -32601;

  public static final int ERROR_INVALID_REQUEST = -32600;

  public static final int ERROR_PARSE_ERROR = -32700;

  public static final int ERROR_INTERNAL_ERROR = -32603;

  public static final int ERROR_SERVER_ERROR = -32000;

}
