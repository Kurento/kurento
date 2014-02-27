${config.subfolder}/MediaPipelineFactory.java
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
package ${config.packageName};

import java.net.InetSocketAddress;

import javax.annotation.PostConstruct;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.google.gson.JsonObject;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.jsonrpcconnector.JsonUtils;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClientThrift;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.thrift.MediaServerCallbackHandler;
import com.kurento.kmf.thrift.pool.MediaServerClientPoolService;
import com.kurento.kms.thrift.api.KmsMediaHandlerService.Iface;
import com.kurento.kms.thrift.api.KmsMediaHandlerService.Processor;
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
public class MediaPipelineFactory {

	private static final Logger log = LoggerFactory
			.getLogger(MediaPipelineFactory.class.getName());

	/**
	 * Processor of KMS calls.
	 */
	private final Processor<Iface> processor = new Processor<Iface>(
			new Iface() {
				@Override
				public String eventJsonRpc(String request) throws TException {

					Request<JsonObject> response = JsonUtils.fromJsonRequest(
							request, JsonObject.class);

					fireEvent("onEvent", response.getParams());

					return null;
				}
			});

	@Autowired
	private MediaServerClientPoolService clientPool;

	/**
	 * Autowired configuration.
	 */
	@Autowired
	private MediaApiConfiguration config;

	/**
	 * Autowired context.
	 */
	@Autowired
	private ApplicationContext ctx;

	/**
	 * Callback handler to be invoked when receiving error and event
	 * notifications from the KMS
	 */
	@Autowired
	private MediaServerCallbackHandler handler;

	private RemoteObjectTypedFactory factory;

	@PostConstruct
	private void init() {
		// TODO stop this handlerServer if the application stops
		ctx.getBean(
				"mediaHandlerServer",
				this.processor,
				new InetSocketAddress(config.getHandlerAddress(), config
						.getHandlerPort()));

		factory = new RemoteObjectTypedFactory(
				new RemoteObjectFactory(new RomClientJsonRpcClient(
						new JsonRpcClientThrift(clientPool))));

	}

	protected void fireEvent(String string, JsonObject params) {
		// TODO Auto-generated method stub

	}

	/**
	 * Creates a new {@link MediaPipeline} in the media server
	 *
	 * @return The media pipeline
	 */
	public MediaPipeline create() {
		return factory.getFactory(MediaPipeline.Factory.class)
				.create().build();
	}

	/**
	 * Creates a new {@link MediaPipeline} in the media server
	 *
	 * @param garbagePeriod
	 * @return The media pipeline
	 */
	public MediaPipeline create(int garbagePeriod) {
		return factory.getFactory(MediaPipeline.Factory.class)
				.create().withGarbagePeriod(garbagePeriod).build();
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

		factory.getFactory(MediaPipeline.Factory.class)
				.create().buildAsync(cont);
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
		factory.getFactory(MediaPipeline.Factory.class)
				.create().withGarbagePeriod(garbagePeriod).buildAsync(cont);
	}

}


