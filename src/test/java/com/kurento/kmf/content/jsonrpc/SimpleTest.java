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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.kurento.kmf.content.jsonrpc.param.JsonRpcConstraints;
import com.kurento.kmf.content.jsonrpc.result.JsonRpcContentEvent;
import com.kurento.kmf.content.jsonrpc.result.JsonRpcControlEvent;
import com.kurento.kmf.content.jsonrpc.result.JsonRpcReason;

public class SimpleTest {

	@Test
	public void testRequests() {
		JsonRpcRequest reqSent = JsonRpcRequest.newStartRequest("fake sdp",
				new JsonRpcConstraints("sendonly", "sendonly"), 9);
		String json = GsonUtils.getGson().toJson(reqSent);
		JsonRpcRequest reqReceived = GsonUtils.getGson().fromJson(json,
				JsonRpcRequest.class);

		assertTrue(reqReceived.getMethod().equals("start"));

		if (!reqSent.toString().equals(reqReceived.toString())) {
			fail("Start requests do not match");
		}

		reqSent = JsonRpcRequest.newExecuteRequest("myCommandType",
				"myComandData", "mySessionId", 9);
		json = GsonUtils.getGson().toJson(reqSent);
		reqReceived = GsonUtils.getGson().fromJson(json, JsonRpcRequest.class);

		assertTrue(reqReceived.getMethod().equals("execute"));

		if (!reqSent.toString().equals(reqReceived.toString())) {
			fail("Execute requests do not match");
		}

		reqSent = JsonRpcRequest.newPollRequest("mySessionId", 9);
		json = GsonUtils.getGson().toJson(reqSent);
		reqReceived = GsonUtils.getGson().fromJson(json, JsonRpcRequest.class);

		assertTrue(reqReceived.getMethod().equals("poll"));

		if (!reqSent.toString().equals(reqReceived.toString())) {
			fail("Poll requests do not match");
		}

		reqSent = JsonRpcRequest.newTerminateRequest(1, "MyErroMsg", "mySessionId", 9);
		json = GsonUtils.getGson().toJson(reqSent);
		reqReceived = GsonUtils.getGson().fromJson(json, JsonRpcRequest.class);

		assertTrue(reqReceived.getMethod().equals("terminate"));

		if (!reqSent.toString().equals(reqReceived.toString())) {
			fail("Terminate requests do not match");
		}
	}

	@Test
	public void testResponses() {
		JsonRpcResponse resSent = JsonRpcResponse.newStartSdpResponse("MySdp",
				"MySessionId", 9);
		String json = GsonUtils.getGson().toJson(resSent);
		JsonRpcResponse resReceived = GsonUtils.getGson().fromJson(json,
				JsonRpcResponse.class);

		assertTrue(resReceived.getResponseResult().getSdp().equals("MySdp"));

		if (!resSent.toString().equals(resReceived.toString())) {
			fail("Start responses do not match");
		}

		resSent = JsonRpcResponse
				.newStartUrlResponse("MyUrl", "MySessionId", 9);
		json = GsonUtils.getGson().toJson(resSent);
		resReceived = GsonUtils.getGson().fromJson(json, JsonRpcResponse.class);

		assertTrue(resReceived.getResponseResult().getUrl().equals("MyUrl"));

		if (!resSent.toString().equals(resReceived.toString())) {
			fail("Start responses do not match");
		}

		resSent = JsonRpcResponse.newStartRejectedResponse(2345,
				"MyRejectedMessage", 9);
		json = GsonUtils.getGson().toJson(resSent);
		resReceived = GsonUtils.getGson().fromJson(json, JsonRpcResponse.class);

		assertTrue(resReceived.getResponseResult().getRejected().getMessage()
				.equals("MyRejectedMessage"));

		if (!resSent.toString().equals(resReceived.toString())) {
			fail("Start responses do not match");
		}

		resSent = JsonRpcResponse.newExecuteResponse("MyComandResult", 9);
		json = GsonUtils.getGson().toJson(resSent);
		resReceived = GsonUtils.getGson().fromJson(json, JsonRpcResponse.class);

		assertTrue(resReceived.getResponseResult().getCommandResult()
				.equals("MyComandResult"));

		if (!resSent.toString().equals(resReceived.toString())) {
			fail("Execute responses do not match");
		}

		resSent = JsonRpcResponse.newError(2345, "MyErrorMessage",
				"MyErrorData", 9);
		json = GsonUtils.getGson().toJson(resSent);
		resReceived = GsonUtils.getGson().fromJson(json, JsonRpcResponse.class);

		assertTrue(resReceived.isError());
		assertTrue(resReceived.getResponseError().getCode() == 2345);

		if (!resSent.toString().equals(resReceived.toString())) {
			fail("Error responses do not match");
		}

		resSent = JsonRpcResponse.newPollResponse(null, null, 9);
		json = GsonUtils.getGson().toJson(resSent);
		resReceived = GsonUtils.getGson().fromJson(json, JsonRpcResponse.class);

		if (!resSent.toString().equals(resReceived.toString())) {
			fail("Poll responses do not match");
		}

		JsonRpcContentEvent[] contentEvents = new JsonRpcContentEvent[10];
		for (int i = 0; i < 10; i++) {
			contentEvents[i] = new JsonRpcContentEvent("MyType" + i, "MyData"
					+ i);
		}

		JsonRpcControlEvent[] controlEvents = new JsonRpcControlEvent[10];
		for (int i = 0; i < 10; i++) {
			controlEvents[i] = new JsonRpcControlEvent("MyType" + i,
					new JsonRpcReason(34, "MyMessage" + i));
		}

		resSent = JsonRpcResponse.newPollResponse(contentEvents, controlEvents,
				9);
		json = GsonUtils.getGson().toJson(resSent);
		resReceived = GsonUtils.getGson().fromJson(json, JsonRpcResponse.class);

		assertTrue(!resReceived.isError());
		assertTrue(resReceived.getResponseResult().getControlEvents()[0]
				.getType().equals("MyType0"));

		if (!resSent.toString().equals(resReceived.toString())) {
			fail("Poll responses do not match");
		}
	}
}
