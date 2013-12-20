package com.kurento.kmf.jsonrpcconnector.internal.ws;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

import org.apache.http.concurrent.BasicFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;

public class PendingRequests {

	private static final Logger log = LoggerFactory
			.getLogger(PendingRequests.class);

	private ConcurrentMap<Integer, BasicFuture<Response<JsonElement>>> pendingRequests = new ConcurrentHashMap<Integer, BasicFuture<Response<JsonElement>>>();

	public void handleResponse(Response<JsonElement> response) {

		BasicFuture<Response<JsonElement>> responseFuture = pendingRequests
				.remove(response.getId());

		if (responseFuture == null) {
			// TODO It is necessary to do something else? Who is watching this?
			log.error("Received response with an id not registered as pending request");
		} else {
			responseFuture.completed(response);
		}
	}

	public Future<Response<JsonElement>> prepareResponse(Integer id) {

		Preconditions.checkNotNull(id, "The request id cannot be null");

		BasicFuture<Response<JsonElement>> responseFuture = new BasicFuture<Response<JsonElement>>(
				null);

		if (pendingRequests.putIfAbsent(id, responseFuture) != null) {
			throw new RuntimeException("Can not send a request with the id '"
					+ id
					+ "'. There is an already pending request with this id");
		}

		return responseFuture;
	}

}
