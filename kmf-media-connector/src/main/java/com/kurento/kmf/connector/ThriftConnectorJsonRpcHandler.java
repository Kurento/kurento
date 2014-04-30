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
package com.kurento.kmf.connector;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.JsonUtils;
import com.kurento.kmf.jsonrpcconnector.Session;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;
import com.kurento.kmf.jsonrpcconnector.internal.message.ResponseError;
import com.kurento.kmf.thrift.ThriftServer;
import com.kurento.kmf.thrift.pool.MediaServerClientPoolService;
import com.kurento.kms.thrift.api.KmsMediaHandlerService.Iface;
import com.kurento.kms.thrift.api.KmsMediaHandlerService.Processor;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.invokeJsonRpc_call;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 1.0.0
 * 
 */
public final class ThriftConnectorJsonRpcHandler extends
		DefaultJsonRpcHandler<JsonObject> {

	private final Logger LOG = LoggerFactory
			.getLogger(ThriftConnectorJsonRpcHandler.class);

	/**
	 * Processor of KMS calls.
	 */
	private final Processor<Iface> processor = new Processor<Iface>(
			new Iface() {
				@Override
				public void eventJsonRpc(String request) throws TException {
					internalEventJsonRpc(request);
				}
			});

	/**
	 * Pool of KMS clients.
	 */
	@Autowired
	private MediaServerClientPoolService clientPool;

	@Autowired
	private ThriftConnectorConfiguration config;

	@Autowired
	private ApplicationContext ctx;

	private ThriftServer server;

	private final ConcurrentMap<String, Session> subscriptions = new ConcurrentHashMap<>();

	@PostConstruct
	private void init() {

		LOG.warn("Handler Address: {}", config.getHandlerAddress());
		LOG.warn("Handler Port: {}", config.getHandlerPort());

		server = (ThriftServer) ctx.getBean(
				"mediaHandlerServer",
				this.processor,
				new InetSocketAddress(config.getHandlerAddress(), config
						.getHandlerPort()));
		server.start();
	}

	@PreDestroy
	private void destroy() {
		if (server != null) {
			server.destroy();
		}
	}

	@Override
	public void afterConnectionEstablished(final Session session)
			throws Exception {

	}

	@Override
	public void afterConnectionClosed(Session session, String status)
			throws Exception {

		Iterator<Entry<String, Session>> it = subscriptions.entrySet()
				.iterator();
		while (it.hasNext()) {
			Entry<String, Session> value = it.next();
			if (value.getValue() == session) {
				it.remove();
			}
		}
	}

	@Override
	public void handleRequest(final Transaction transaction,
			final Request<JsonObject> request) throws Exception {
		transaction.startAsync();

		sendRequest(transaction, request, true);

	}

	private void sendRequest(final Transaction transaction,
			final Request<JsonObject> request, final boolean retry) {

		final AsyncClient client = clientPool.acquireAsync();
		final boolean subscribeRequest;

		if (request.getMethod().equals("subscribe")) {
			request.getParams().addProperty("ip", config.getHandlerAddress());
			request.getParams().addProperty("port", config.getHandlerPort());
			subscribeRequest = true;
		} else {
			subscribeRequest = false;
		}

		try {
			client.invokeJsonRpc(request.toString(),
					new AsyncMethodCallback<invokeJsonRpc_call>() {

						@Override
						public void onComplete(invokeJsonRpc_call response) {
							clientPool.release(client);
							if (request.getId() != null) {
								requestOnComplete(response, transaction,
										subscribeRequest);
							}
						}

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);

							LOG.error("Error on release", exception);
							if (retry && exception instanceof ConnectException) {
								sendRequest(transaction, request, false);
							} else {
								requestOnError(exception, transaction);
							}
						}
					});
		} catch (Exception e) {
			LOG.error("Exception while executing a command"
					+ " in thrift interface of the MediaServer", e);
		}
	}

	protected void requestOnError(Exception exception, Transaction transaction) {
		try {
			transaction.sendError(exception);
		} catch (IOException e) {
			// TODO Error code
			throw new KurentoMediaFrameworkException(
					"Exception while sending response to client");
		}
	}

	protected void requestOnComplete(invokeJsonRpc_call mediaServerResponse,
			Transaction transaction, boolean subscribeResponse) {

		try {

			String result = mediaServerResponse.getResult();

			Response<JsonElement> response = JsonUtils.fromJsonResponse(result,
					JsonElement.class);

			if (response.isError()) {
				ResponseError error = response.getError();
				transaction.sendError(error.getCode(), error.getMessage(),
						error.getData());
			} else {

				if (subscribeResponse) {
					try {
						String subscription = ((JsonObject) response
								.getResult()).get("value").getAsString().trim();
						subscriptions.put(subscription,
								transaction.getSession());
					} catch (Exception e) {
						LOG.error("Error getting subscription on response {}",
								response, e);
					}
				}

				transaction.sendResponse(response.getResult());
			}

		} catch (TException e) {

			try {

				transaction.sendError(e);

			} catch (IOException e1) {
				// TODO Error code
				throw new KurentoMediaFrameworkException(
						"Exception while sending response to client");
			}

		} catch (IOException e) {
			// TODO Error code
			throw new KurentoMediaFrameworkException(
					"Exception while sending response to client");
		}
	}

	private void internalEventJsonRpc(String request) {
		try {

			LOG.debug("<-* {}", request.trim());

			Request<JsonObject> requestObj = JsonUtils.fromJsonRequest(request,
					JsonObject.class);

			JsonElement subsJsonElem = requestObj.getParams().get("value")
					.getAsJsonObject().get("subscription");

			if (subsJsonElem == null) {
				LOG.error("Received event wihthout subscription: {}", request);
				return;
			}

			String subscription = subsJsonElem.getAsString().trim();
			Session session = subscriptions.get(subscription);
			if (session == null) {
				LOG.error("Unknown subscription: \"{}\"", subscriptions);
				LOG.info("Subscriptions\n");
				return;
			}

			try {

				session.sendNotification("onEvent", requestObj.getParams());

			} catch (Exception e) {
				LOG.error("Exception while sending event", e);
			}

		} catch (Exception e) {
			LOG.error("Exception processing server event", e);
		}
	}
}
