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
package org.kurento.client;

import java.io.IOException;

import javax.annotation.PreDestroy;

import org.kurento.client.internal.TransactionImpl;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory to create {@link MediaPipeline} in the media server.
 *
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public class KurentoClient {

	private static Logger log = LoggerFactory.getLogger(KurentoClient.class);

	protected RomManager manager;

	public static KurentoClient create(String websocketUrl) {
		log.debug("Connecting to kms in uri " + websocketUrl);
		return new KurentoClient(new JsonRpcClientWebSocket(websocketUrl));
	}

	public static KurentoClient create(String websocketUrl,
			KurentoConnectionListener listener) {
		log.info("Connecting to KMS in "+websocketUrl);
		return new KurentoClient(new JsonRpcClientWebSocket(websocketUrl,
				JsonRpcConnectionListenerKurento.create(listener)));

	}

	KurentoClient(JsonRpcClient client) {
		this.manager = new RomManager(new RomClientJsonRpcClient(client));
		try {
			client.connect();
		} catch (IOException e){
			throw new KurentoException("Exception connecting to KMS",e);
		}
	}

	/**
	 * Creates a new {@link MediaPipeline} in the media server
	 *
	 * @return The media pipeline
	 */
	public MediaPipeline createMediaPipeline() {
		return new AbstractBuilder<MediaPipeline>(MediaPipeline.class, manager)
				.build();
	}

	/**
	 * Creates a new {@link MediaPipeline} in the media server
	 *
	 * @param cont
	 *            An asynchronous callback handler. If the element was
	 *            successfully created, the {@code onSuccess} method from the
	 *            handler will receive a {@link MediaPipeline} stub from the
	 *            media server.
	 * @throws KurentoException
	 *
	 */
	public void createMediaPipeline(final Continuation<MediaPipeline> cont)
			throws KurentoException {
		new AbstractBuilder<MediaPipeline>(MediaPipeline.class, manager)
				.buildAsync(cont);
	}

	public MediaPipeline createMediaPipeline(Transaction tx) {
		return new AbstractBuilder<MediaPipeline>(MediaPipeline.class, manager)
				.build(tx);
	}

	@PreDestroy
	public void destroy() {
		manager.destroy();
	}

	public static KurentoClient createFromJsonRpcClient(
			JsonRpcClient jsonRpcClient) {
		return new KurentoClient(jsonRpcClient);
	}

	public Transaction beginTransaction() {
		return new TransactionImpl(manager);
	}

}
