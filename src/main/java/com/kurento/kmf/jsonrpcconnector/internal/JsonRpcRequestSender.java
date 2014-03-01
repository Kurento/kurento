package com.kurento.kmf.jsonrpcconnector.internal;

import java.io.IOException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kurento.kmf.jsonrpcconnector.client.Continuation;

public interface JsonRpcRequestSender {

	public <R> R sendRequest(String method, Class<R> resultClass)
			throws IOException;

	public <R> R sendRequest(String method, Object params, Class<R> resultClass)
			throws IOException;

	public JsonElement sendRequest(String method) throws IOException;

	public JsonElement sendRequest(String method, Object params)
			throws IOException;

	public void sendNotification(String method, Object params)
			throws IOException;

	public void sendNotification(String method) throws IOException;

	void sendRequest(String method, JsonObject params,
			Continuation<JsonElement> continuation);

	void sendNotification(String method, Object params,
			Continuation<JsonElement> continuation) throws IOException;
}
