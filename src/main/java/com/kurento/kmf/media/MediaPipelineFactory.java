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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.thrift.internal.ThriftInterfaceExecutorService;
import com.kurento.kmf.thrift.jsonrpcconnector.JsonRpcClientThrift;
import com.kurento.kmf.thrift.pool.MediaServerClientPoolService;
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

	@Autowired
	private MediaApiConfiguration config;

	@Autowired
	private MediaServerClientPoolService clientPool;

	@Autowired
	private ThriftInterfaceExecutorService executorService;

	private RemoteObjectTypedFactory factory;

	@PostConstruct
	private void init() {

		JsonRpcClient client = new JsonRpcClientThrift(clientPool,
				executorService, new InetSocketAddress(
						config.getHandlerAddress(), config.getHandlerPort()));

		factory = new RemoteObjectTypedFactory(new RemoteObjectFactory(
				new RomClientJsonRpcClient(client)));
	}
	
	@PreDestroy
	private void destroy() {
		factory.destroy();
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
	 * @param garbagePeriod
	 * @return The media pipeline
	 */
	public MediaPipeline create(int garbagePeriod) {
		return factory.getFactory(MediaPipeline.Factory.class).create()
				.withGarbagePeriod(garbagePeriod).build();
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

	/**
	 * Creates a new {@link MediaPipeline} in the media server
	 * 
	 * @param garbagePeriod
	 * @param cont
	 *            An asynchronous callback handler. If the element was
	 *            successfully created, the {@code onSuccess} method from the
	 *            handler will receive a {@link MediaPipeline} stub from the
	 *            media server.
	 * @throws KurentoMediaFrameworkException
	 * 
	 */
	public void create(int garbagePeriod, final Continuation<MediaPipeline> cont)
			throws KurentoMediaFrameworkException {
		factory.getFactory(MediaPipeline.Factory.class).create()
				.withGarbagePeriod(garbagePeriod).buildAsync(cont);
	}

}
