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
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client of Kurento Server
 *
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 2.0.0
 */
public class KurentoClient {

	private static Logger log = LoggerFactory.getLogger(KurentoClient.class);

	protected RomManager manager;

	public static KurentoClient create(String websocketUrl) throws IOException {
		log.debug("Connecting to kms in uri " + websocketUrl);
		return new KurentoClient(new JsonRpcClientWebSocket(websocketUrl));
	}

	public static KurentoClient create(String websocketUrl,
			KurentoConnectionListener listener) throws IOException {
		return new KurentoClient(new JsonRpcClientWebSocket(websocketUrl,
				JsonRpcConnectionListenerKurento.create(listener)));
	}

	@PreDestroy
	public void destroy() {
		manager.destroy();
	}

	KurentoClient(JsonRpcClient client) throws IOException {
		client.connect();
		this.manager = new RomManager(new RomClientJsonRpcClient(client));
	}

	KurentoClient(RomManager manager) {
		this.manager = manager;
	}

	RomManager getRomManager() {
		return manager;
	}

	public static KurentoClient createFromJsonRpcClient(
			JsonRpcClient jsonRpcClient) throws IOException {
		return new KurentoClient(jsonRpcClient);
	}

	public Transaction beginTransaction() {
		return new TransactionImpl(manager);
	}
}
