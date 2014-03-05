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
import java.net.InetSocketAddress;

import javax.annotation.PostConstruct;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.google.gson.JsonObject;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.JsonUtils;
import com.kurento.kmf.jsonrpcconnector.Session;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;
import com.kurento.kmf.jsonrpcconnector.internal.message.ResponseError;
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
					try {

						Request<JsonObject> requestObj = JsonUtils
								.fromJsonRequest(request, JsonObject.class);

						try {

							session.sendNotification("onEvent",
									requestObj.getParams());

						} catch (Exception e) {
							LOG.error("Exception while sending event", e);
						}

					} catch (Exception e) {
						throw new KurentoMediaFrameworkException(
								"Exception processing server event", e);
					}
				}

			});

	private Session session;

	/**
	 * Pool of KMS clients.
	 */
	@Autowired
	private MediaServerClientPoolService clientPool;

	@Autowired
	private ThriftConnectorConfiguration config;

	@Autowired
	private ApplicationContext ctx;

	@PostConstruct
	private void init() {
		ctx.getBean(
				"mediaHandlerServer",
				this.processor,
				new InetSocketAddress(config.getHandlerAddress(), config
						.getHandlerPort()));
	}

	@Override
	public void afterConnectionEstablished(final Session session)
			throws Exception {
		this.session = session;
	}

	@Override
	public void handleRequest(final Transaction transaction,
			final Request<JsonObject> request) throws Exception {

		final AsyncClient client = clientPool.acquireAsync();

		transaction.startAsync();

		if (request.getMethod().equals("subscribe")) {
			request.getParams().addProperty("ip", config.getHandlerAddress());
			request.getParams().addProperty("port", config.getHandlerPort());
		}

		client.invokeJsonRpc(request.toString(),
				new AsyncMethodCallback<invokeJsonRpc_call>() {

					@Override
					public void onComplete(invokeJsonRpc_call response) {
						clientPool.release(client);

						if (request.getId() != null)
							requestOnComplete(response, transaction);
					}

					@Override
					public void onError(Exception exception) {
						clientPool.release(client);
						requestOnError(exception, transaction);
					}
				});
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
			Transaction transaction) {

		try {

			String result = mediaServerResponse.getResult();

			Response<JsonObject> response = JsonUtils.fromJsonResponse(result,
					JsonObject.class);

			if (response.isError()) {
				ResponseError error = response.getError();
				transaction.sendError(error.getCode(), error.getMessage(),
						error.getData());
			} else {
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
}
