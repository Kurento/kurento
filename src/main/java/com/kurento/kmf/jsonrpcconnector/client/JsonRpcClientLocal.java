package com.kurento.kmf.jsonrpcconnector.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.kurento.kmf.jsonrpcconnector.JsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.JsonUtils;
import com.kurento.kmf.jsonrpcconnector.internal.JsonRpcRequestSenderHelper;
import com.kurento.kmf.jsonrpcconnector.internal.client.ClientSession;
import com.kurento.kmf.jsonrpcconnector.internal.message.Message;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;
import com.kurento.kmf.jsonrpcconnector.internal.message.ResponseError;
import com.kurento.kmf.jsonrpcconnector.internal.server.TransactionImpl;
import com.kurento.kmf.jsonrpcconnector.internal.server.TransactionImpl.ResponseSender;

public class JsonRpcClientLocal extends JsonRpcClient {

	private static Logger LOG = LoggerFactory
			.getLogger(JsonRpcClientLocal.class);

	private JsonRpcHandler<JsonObject> handler;

	public <F> JsonRpcClientLocal(JsonRpcHandler<JsonObject> paramHandler) {

		this.handler = paramHandler;

		session = new ClientSession("XXX", null, this);

		rsHelper = new JsonRpcRequestSenderHelper() {
			@SuppressWarnings("unchecked")
			@Override
			public <P, R> Response<R> internalSendRequest(Request<P> request,
					Class<R> resultClass) throws IOException {

				// Simulate sending json string for net
				String jsonRequest = request.toString();

				LOG.debug("> " + jsonRequest);

				Request<JsonObject> newRequest = JsonUtils.fromJsonRequest(
						jsonRequest, JsonObject.class);

				final Response<JsonObject>[] response = new Response[1];

				TransactionImpl t = new TransactionImpl(session, newRequest,
						new ResponseSender() {
							@Override
							public void sendResponse(Message message)
									throws IOException {
								response[0] = (Response<JsonObject>) message;
							}
						});

				try {
					handler.handleRequest(t, (Request<JsonObject>) request);
				} catch (Exception e) {

					ResponseError error = ResponseError.newFromException(e);
					return new Response<R>(request.getId(), error);
				}

				if (response[0] != null) {
					// Simulate receiving json string from net
					String jsonResponse = response[0].toString();

					LOG.debug("< " + jsonResponse);

					Response<R> newResponse = JsonUtils.fromJsonResponse(
							jsonResponse, resultClass);

					newResponse.setId(request.getId());

					return newResponse;

				} else {
					return new Response<R>(request.getId());
				}
			}
		};
	}

	@Override
	public void close() throws IOException {

	}

}
