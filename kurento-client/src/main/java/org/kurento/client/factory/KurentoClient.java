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
package org.kurento.client.factory;

import javax.annotation.PreDestroy;

import org.kurento.client.AbstractBuilder;
import org.kurento.client.Continuation;
import org.kurento.client.MediaPipeline;
import org.kurento.client.internal.client.RemoteObjectFactory;
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

	protected RemoteObjectFactory factory;

	public static KurentoClient create(String websocketUrl) {
		log.debug("Connecting to kms in uri " + websocketUrl);
		return new KurentoClient(new JsonRpcClientWebSocket(websocketUrl));
	}

	KurentoClient(JsonRpcClient client) {
		this.factory = new RemoteObjectFactory(new RomClientJsonRpcClient(
				client));
	}

	/**
	 * Creates a new {@link MediaPipeline} in the media server
	 *
	 * @return The media pipeline
	 */
	public MediaPipeline createMediaPipeline() {
		return new AbstractBuilder<MediaPipeline>(MediaPipeline.class, factory)
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
		new AbstractBuilder<MediaPipeline>(MediaPipeline.class, factory)
				.buildAsync(cont);
	}

	@PreDestroy
	public void destroy() {
		factory.destroy();
	}

	public static KurentoClient createFromJsonRpcClient(
			JsonRpcClient jsonRpcClient) {
		return new KurentoClient(jsonRpcClient);
	}

}
