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
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;

import javax.annotation.PostConstruct;

import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.google.gson.JsonObject;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.Session;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.thrift.pool.MediaServerClientPoolService;
import com.kurento.kms.thrift.api.KmsMediaError;
import com.kurento.kms.thrift.api.KmsMediaEvent;
import com.kurento.kms.thrift.api.KmsMediaHandlerService.Iface;
import com.kurento.kms.thrift.api.KmsMediaHandlerService.Processor;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 1.0.0
 * 
 */
public final class ThriftConnectorJsonRpcHandler extends
		DefaultJsonRpcHandler<JsonObject> {

	/**
	 * Processor of KMS calls.
	 */
	private final Processor<Iface> processor = new Processor<Iface>(
			new Iface() {

				@Override
				public void onError(final String callbackToken,
						final KmsMediaError error) throws TException {
					// TODO convert error to protocol message
					try {
						session.sendRequest("onError", error);
					} catch (IOException e) {
						// TODO log and change exception type
						throw new KurentoMediaFrameworkException("");
					}
				}

				@Override
				public void onEvent(final String callbackToken,
						final KmsMediaEvent event) throws TException {
					// TODO convert event to protocol message
					try {
						session.sendRequest("onEvent", event);
					} catch (IOException e) {
						// TODO log and change exception type
						throw new KurentoMediaFrameworkException("");
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
	private MethodResolver methodResolver;

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

		ThriftMethod method = methodResolver.lookup(request.getMethod(),
				request.getParams());

		AsyncClient client = clientPool.acquireAsync();

		try {

			transaction.startAsync();
			method.invoke(client, request.getParams(), transaction);

		} catch (InvocationTargetException e) {

			clientPool.release(client);
			// TODO add error code
			throw new KurentoMediaFrameworkException(
					"An exception occurred in the Media Server invoking the method "
							+ request.getMethod());

		} catch (Throwable e) {
			clientPool.release(client);
			throw new KurentoMediaFrameworkException(e.getMessage(), e);
		}
	}

}
