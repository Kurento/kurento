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
package com.kurento.kmf.jsonrpcconnector.internal.ws;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

import org.apache.http.concurrent.BasicFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.kurento.kmf.jsonrpcconnector.JsonRpcConnectorException;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;

public class PendingRequests {

	private static final Logger log = LoggerFactory
			.getLogger(PendingRequests.class);

	private final ConcurrentMap<Integer, BasicFuture<Response<JsonElement>>> pendingRequests = new ConcurrentHashMap<>();

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

		BasicFuture<Response<JsonElement>> responseFuture = new BasicFuture<>(
				null);

		if (pendingRequests.putIfAbsent(id, responseFuture) != null) {
			throw new JsonRpcConnectorException(
					"Can not send a request with the id '"
							+ id
							+ "'. There is already a pending request with this id");
		}

		return responseFuture;
	}

}
