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
package com.kurento.kmf.media;

import java.net.InetSocketAddress;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.thrift.ThriftInterfaceConfiguration;
import com.kurento.kmf.thrift.internal.ThriftInterfaceExecutorService;
import com.kurento.kmf.thrift.jsonrpcconnector.JsonRpcClientThrift;
import com.kurento.kmf.thrift.pool.MediaServerAsyncClientFactory;
import com.kurento.kmf.thrift.pool.MediaServerAsyncClientPool;
import com.kurento.kmf.thrift.pool.MediaServerClientPoolService;
import com.kurento.kmf.thrift.pool.MediaServerSyncClientFactory;
import com.kurento.kmf.thrift.pool.MediaServerSyncClientPool;
import com.kurento.tool.rom.client.RemoteObjectFactory;
import com.kurento.tool.rom.client.RemoteObjectTypedFactory;
import com.kurento.tool.rom.transport.jsonrpcconnector.RomClientJsonRpcClient;

/**
 * Factory to create {@link MediaPipeline} in the media server.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
@Component
public class MediaPipelineFactory {

	private static final Logger log = LoggerFactory
			.getLogger(MediaPipelineFactory.class);

	@Autowired
	private MediaApiConfiguration config;

	@Autowired
	private MediaServerClientPoolService clientPool;

	@Autowired
	private ThriftInterfaceExecutorService executorService;

	private RemoteObjectTypedFactory factory;

	private boolean springEnv = true;

	private JsonRpcClient client;

	// Used in Spring environments
	public MediaPipelineFactory() {
	}

	// Used in non Spring environments
	public MediaPipelineFactory(String serverAddress, int serverPort,
			String handlerAddress, int handlerPort) {

		log.info(
				"Creating pipeline factory in non-spring environment with server {}:{} and handler {}:{}",
				serverAddress, serverPort, handlerAddress, handlerPort);

		this.config = new MediaApiConfiguration();

		this.config.setHandlerAddress(handlerAddress);
		this.config.setHandlerPort(handlerPort);

		ThriftInterfaceConfiguration cfg = new ThriftInterfaceConfiguration(
				serverAddress, serverPort);

		MediaServerAsyncClientPool asyncClientPool = new MediaServerAsyncClientPool(
				new MediaServerAsyncClientFactory(cfg), cfg);

		MediaServerSyncClientPool syncClientPool = new MediaServerSyncClientPool(
				new MediaServerSyncClientFactory(cfg), cfg);

		this.clientPool = new MediaServerClientPoolService(asyncClientPool,
				syncClientPool);

		this.executorService = new ThriftInterfaceExecutorService(cfg);

		this.springEnv = false;

		init();
	}

	// Used in non Spring environments
	public MediaPipelineFactory(JsonRpcClient client) {
		this.client = client;
		init();
	}

	@PostConstruct
	private void init() {

		if(client == null){
			this.client = new JsonRpcClientThrift(clientPool,
					executorService, new InetSocketAddress(
							config.getHandlerAddress(), config.getHandlerPort()));			
		}

		factory = new RemoteObjectTypedFactory(new RemoteObjectFactory(
				new RomClientJsonRpcClient(client)));
	}

	@PreDestroy
	public void destroy() {
		factory.destroy();
		if (!springEnv) {
			executorService.destroy();
		}
	}

	/**
	 * Creates a new {@link MediaPipeline} in the media server
	 * 
	 * @return The media pipeline
	 */
	public MediaPipeline create() {
		return factory.getFactory(MediaPipeline.Factory.class).create().build();
	}

	/**
	 * Creates a new {@link MediaPipeline} in the media server
	 * 
	 * @param cont
	 *            An asynchronous callback handler. If the element was
	 *            successfully created, the {@code onSuccess} method from the
	 *            handler will receive a {@link MediaPipeline} stub from the
	 *            media server.
	 * @throws KurentoMediaFrameworkException
	 * 
	 */
	public void create(final Continuation<MediaPipeline> cont)
			throws KurentoMediaFrameworkException {

		factory.getFactory(MediaPipeline.Factory.class).create()
				.buildAsync(cont);
	}

}
