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
package com.kurento.kmf.content.jsonrpc;

import com.kurento.kmf.content.jsonrpc.param.JsonRpcCommand;
import com.kurento.kmf.content.jsonrpc.param.JsonRpcConstraints;
import com.kurento.kmf.content.jsonrpc.param.JsonRpcRequestParams;

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
	private String jsonrpc = JsonRpcConstants.JSON_RPC_VERSION;

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
	private Integer id;

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
	public static JsonRpcRequest newStartRequest(String sdp,
			JsonRpcConstraints constraints, Integer id) {
		return new JsonRpcRequest(JsonRpcConstants.METHOD_START,
				JsonRpcRequestParams.newStartRequestParams(sdp, constraints),
				id);
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
	// TODO: for symmetry rename rest of newRequest methods?
	public static JsonRpcRequest newExecuteRequest(String commandType,
			String commandData, String sessionId, Integer id) {
		return new JsonRpcRequest(JsonRpcConstants.METHOD_EXECUTE,
				JsonRpcRequestParams
						.newExecuteRequestParams(new JsonRpcCommand(
								commandType, commandData), sessionId), id);
	}

	public static JsonRpcRequest newPollRequest(String sessionId, Integer id) {
		return new JsonRpcRequest(JsonRpcConstants.METHOD_POLL,
				JsonRpcRequestParams.newPollRequestParams(sessionId), id);
	}

	public static JsonRpcRequest newTerminateRequest(Integer code,
			String message, String sessionId, Integer id) {
		return new JsonRpcRequest(JsonRpcConstants.METHOD_TERMINATE,
				JsonRpcRequestParams.newTerminateRequestParams(code, message,
						sessionId), id);
	}

	/**
	 * Default constructor.
	 */
	public JsonRpcRequest() {
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
	JsonRpcRequest(String method, JsonRpcRequestParams params, Integer id) {
		this.method = method;
		this.params = params;
		this.id = id;
	}

	public String getVersion() {
		return jsonrpc;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public JsonRpcRequestParams getParams() {
		return params;
	}

	public void setParams(JsonRpcRequestParams params) {
		this.params = params;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return GsonUtils.toString(this);
	}
}