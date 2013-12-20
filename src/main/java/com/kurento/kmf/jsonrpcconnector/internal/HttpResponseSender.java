package com.kurento.kmf.jsonrpcconnector.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.kurento.kmf.jsonrpcconnector.internal.message.Message;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;
import com.kurento.kmf.jsonrpcconnector.internal.server.TransactionImpl.ResponseSender;

public class HttpResponseSender implements ResponseSender {

	private List<Response<Object>> responses = new ArrayList<Response<Object>>();

	public synchronized List<Response<Object>> getResponseListToSend() {
		List<Response<Object>> returnResponses = responses;
		responses = new ArrayList<Response<Object>>();
		return returnResponses;
	}

	@Override
	public synchronized void sendResponse(Message message) throws IOException {
		responses.add((Response<Object>) message);
	}
}
