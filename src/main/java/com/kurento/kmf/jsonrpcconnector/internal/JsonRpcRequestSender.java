package com.kurento.kmf.jsonrpcconnector.internal;

import java.io.IOException;

import com.google.gson.JsonElement;

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
}
