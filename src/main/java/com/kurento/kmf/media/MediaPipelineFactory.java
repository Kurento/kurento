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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.events.MediaError;
import com.kurento.kmf.media.events.MediaEvent;
import com.kurento.kmf.media.internal.ErrorListenerRegistration;
import com.kurento.kmf.media.internal.EventListenerRegistration;
import com.kurento.kmf.media.internal.MediaPipelineImpl;
import com.kurento.kmf.media.internal.MediaServerCallbackHandler;
import com.kurento.kmf.media.internal.refs.MediaPipelineRef;
import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kmf.media.params.internal.AbstractMediaParam;
import com.kurento.kmf.media.params.internal.MediaObjectConstructorParam;
import com.kurento.kmf.thrift.pool.MediaServerClientPoolService;
import com.kurento.kms.thrift.api.KmsMediaError;
import com.kurento.kms.thrift.api.KmsMediaEvent;
import com.kurento.kms.thrift.api.KmsMediaHandlerService;
import com.kurento.kms.thrift.api.KmsMediaHandlerService.Iface;
import com.kurento.kms.thrift.api.KmsMediaHandlerService.Processor;
import com.kurento.kms.thrift.api.KmsMediaObjectConstants;
import com.kurento.kms.thrift.api.KmsMediaParam;
import com.kurento.kms.thrift.api.KmsMediaServerException;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.createMediaPipelineWithParams_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.createMediaPipeline_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.Client;

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

	private final Processor<KmsMediaHandlerService.Iface> processor = new Processor<Iface>(
			new Iface() {

				@Override
				public void onError(String callbackToken, KmsMediaError error)
						throws TException {
					log.trace("KMS error {} received on object {}",
							Integer.toString(error.errorCode),
							Long.toString(error.getSource().getId()));
					MediaError mediaError = (MediaError) ctx.getBean(
							"mediaError", error);
					ErrorListenerRegistration registration = new ErrorListenerRegistration(
							callbackToken);
					handler.onError(registration,
							Long.valueOf(error.getSource().id), mediaError);
				}

				@Override
				public void onEvent(String callbackToken, KmsMediaEvent event)
						throws TException {
					log.trace("KMS event {} received on object {}", event.type,
							Long.toString(event.getSource().getId()));
					MediaEvent mediaEvent = (MediaEvent) ctx.getBean(
							"mediaEvent", event);

					EventListenerRegistration registration = new EventListenerRegistration(
							callbackToken);
					handler.onEvent(registration,
							Long.valueOf(event.getSource().id), mediaEvent);
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

	@PostConstruct
	private void init() {
		// TODO stop this handlerServer if the application stops
		ctx.getBean(
				"mediaHandlerServer",
				this.processor,
				new InetSocketAddress(config.getHandlerAddress(), config
						.getHandlerPort()));
	}

	/**
	 * Creates a new {@link MediaPipeline} in the media server
	 * 
	 * @return The media pipeline
	 */
	public MediaPipeline create() {
		Client client = this.clientPool.acquireSync();

		MediaPipelineRef pipelineRefDTO;
		try {
			pipelineRefDTO = new MediaPipelineRef(client.createMediaPipeline());
		} catch (KmsMediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			this.clientPool.release(client);
		}

		MediaPipelineImpl pipeline = (MediaPipelineImpl) ctx.getBean(
				"mediaObject", pipelineRefDTO);
		return pipeline;
	}

	/**
	 * Creates a new {@link MediaPipeline} in the media server
	 * 
	 * @param garbagePeriod
	 * @return The media pipeline
	 */
	public MediaPipeline create(int garbagePeriod) {
		Map<String, MediaParam> params = new HashMap<String, MediaParam>(3);

		MediaObjectConstructorParam mocp = new MediaObjectConstructorParam();
		mocp.setGarbageCollectorPeriod(garbagePeriod);
		params.put(KmsMediaObjectConstants.CONSTRUCTOR_PARAMS_DATA_TYPE, mocp);

		return create(params);
	}

	/**
	 * Creates a new {@link MediaPipeline} in the media server
	 * 
	 * @param params
	 *            Parameters to be used by the server to configure the object
	 *            during creation
	 * @return The media pipeline
	 * @throws KurentoMediaFrameworkException
	 */
	public MediaPipeline create(Map<String, MediaParam> params)
			throws KurentoMediaFrameworkException {
		MediaPipeline pipeline;

		if (params == null || params.isEmpty()) {
			pipeline = create();
		} else {
			MediaPipelineRef pipelineRef;
			Client client = this.clientPool.acquireSync();
			try {
				// TODO Add real params map
				pipelineRef = new MediaPipelineRef(
						client.createMediaPipelineWithParams(transformMediaParamsMap(params)));
			} catch (KmsMediaServerException e) {
				throw new KurentoMediaFrameworkException(e.getMessage(), e,
						e.getErrorCode());
			} catch (TException e) {
				// TODO change error code
				throw new KurentoMediaFrameworkException(e.getMessage(), e,
						30000);
			} finally {
				this.clientPool.release(client);
			}

			pipeline = (MediaPipelineImpl) ctx.getBean("mediaObjectWithParams",
					pipelineRef, params);
		}

		return pipeline;
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
		final AsyncClient client = this.clientPool.acquireAsync();

		try {
			client.createMediaPipeline(new AsyncMethodCallback<createMediaPipeline_call>() {

				@Override
				public void onError(Exception exception) {
					clientPool.release(client);
				}

				@Override
				public void onComplete(createMediaPipeline_call response) {
					MediaPipelineRef pipelineRefDTO;
					try {
						pipelineRefDTO = new MediaPipelineRef(response
								.getResult());
					} catch (KmsMediaServerException e) {
						throw new KurentoMediaFrameworkException(
								e.getMessage(), e, e.getErrorCode());
					} catch (TException e) {
						// TODO change error code
						throw new KurentoMediaFrameworkException(
								e.getMessage(), e, 30000);
					} finally {
						clientPool.release(client);
					}
					MediaPipelineImpl pipeline = (MediaPipelineImpl) ctx
							.getBean("mediaObject", pipelineRefDTO);
					cont.onSuccess(pipeline);
				}
			});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

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
		final Map<String, MediaParam> params = new HashMap<String, MediaParam>(
				3);

		MediaObjectConstructorParam mocp = new MediaObjectConstructorParam();
		mocp.setGarbageCollectorPeriod(garbagePeriod);
		params.put(KmsMediaObjectConstants.CONSTRUCTOR_PARAMS_DATA_TYPE, mocp);

		create(params, cont);
	}

	/**
	 * Creates a new {@link MediaPipeline} in the media server
	 * 
	 * @param params
	 *            Parameters to be used by the server to configure the object
	 *            during creation
	 * @param cont
	 *            An asynchronous callback handler. If the element was
	 *            successfully created, the {@code onSuccess} method from the
	 *            handler will receive a {@link MediaPipeline} stub from the
	 *            media server.
	 * @throws KurentoMediaFrameworkException
	 * 
	 */
	public void create(final Map<String, MediaParam> params,
			final Continuation<MediaPipeline> cont)
			throws KurentoMediaFrameworkException {

		if (params == null || params.isEmpty()) {
			create(cont);
		} else {

			final AsyncClient client = this.clientPool.acquireAsync();

			try {
				client.createMediaPipelineWithParams(
						transformMediaParamsMap(params),
						new AsyncMethodCallback<createMediaPipelineWithParams_call>() {

							@Override
							public void onError(Exception exception) {
								clientPool.release(client);
							}

							@Override
							public void onComplete(
									createMediaPipelineWithParams_call response) {
								MediaPipelineRef pipelineRefDTO;
								try {
									pipelineRefDTO = new MediaPipelineRef(
											response.getResult());
								} catch (KmsMediaServerException e) {
									throw new KurentoMediaFrameworkException(e
											.getMessage(), e, e.getErrorCode());
								} catch (TException e) {
									// TODO change error code
									throw new KurentoMediaFrameworkException(e
											.getMessage(), e, 30000);
								} finally {
									clientPool.release(client);
								}
								MediaPipelineImpl pipeline = (MediaPipelineImpl) ctx
										.getBean("mediaObjectWithParams",
												pipelineRefDTO, params);
								cont.onSuccess(pipeline);
							}
						});
			} catch (TException e) {
				clientPool.release(client);
				// TODO change error code
				throw new KurentoMediaFrameworkException(e.getMessage(), e,
						30000);
			}
		}
	}

	private Map<String, KmsMediaParam> transformMediaParamsMap(
			Map<String, MediaParam> params) {
		// hashMap size taking into account load factor
		int mapSize = 1 + (int) (params.size() / 0.75);
		Map<String, KmsMediaParam> kmsParams = new HashMap<String, KmsMediaParam>(
				mapSize);

		for (Entry<String, MediaParam> entry : params.entrySet()) {
			kmsParams.put(entry.getKey(),
					((AbstractMediaParam) entry.getValue()).getThriftParams());
		}
		return kmsParams;
	}

}
