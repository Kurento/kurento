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
package com.kurento.kmf.media.internal;

import static com.kurento.kmf.media.internal.refs.MediaRefConverter.fromThrift;

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
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.ListenerRegistration;
import com.kurento.kmf.media.MediaApiConfiguration;
import com.kurento.kmf.media.MediaObject;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.events.MediaErrorListener;
import com.kurento.kmf.media.events.MediaEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.internal.pool.MediaServerClientPoolService;
import com.kurento.kmf.media.internal.refs.MediaObjectRef;
import com.kurento.kmf.media.internal.refs.MediaPipelineRef;
import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kmf.media.params.internal.AbstractMediaParam;
import com.kurento.kms.thrift.api.KmsMediaParam;
import com.kurento.kms.thrift.api.KmsMediaServerException;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.getMediaPipeline_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.getParent_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.invoke_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.release_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.subscribeError_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.subscribeEvent_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.unsubscribeError_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.unsubscribeEvent_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.Client;

public abstract class AbstractMediaObject implements MediaObject {

	private static final Logger log = LoggerFactory
			.getLogger(MediaServerClientPoolService.class);

	protected final MediaObjectRef objectRef;

	@Autowired
	protected MediaServerClientPoolService clientPool;

	@Autowired
	protected ApplicationContext ctx;

	@Autowired
	private MediaServerCallbackHandler handler;

	@Autowired
	private MediaApiConfiguration config;

	private MediaPipelineImpl pipeline;

	private MediaObject parent;

	public MediaObjectRef getObjectRef() {
		return objectRef;
	}

	public AbstractMediaObject(MediaObjectRef ref) {
		objectRef = ref.deepCopy();
	}

	public AbstractMediaObject(MediaObjectRef ref,
			Map<String, MediaParam> params) {
		this(ref);
	}

	public Long getId() {
		return objectRef.getId();
	}

	@PostConstruct
	protected void init() {
		// This method is left blank, and intended to be used by future
		// expansions.
	}

	@Override
	public void release() {

		handler.removeAllListeners(this);

		Client client = clientPool.acquireSync();

		try {
			client.release(this.objectRef.getThriftRef());
		} catch (KmsMediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}

		log.debug("Object {0}: released", getId());
	}

	@Override
	public <E extends MediaEvent> ListenerRegistration addListener(
			String eventType, MediaEventListener<E> listener) {
		Client client = clientPool.acquireSync();

		String callbackToken;

		try {
			callbackToken = client.subscribeEvent(objectRef.getThriftRef(),
					eventType, config.getHandlerAddress(),
					config.getHandlerPort());
		} catch (KmsMediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}

		EventListenerRegistration reg = new EventListenerRegistration(
				callbackToken);
		handler.addListener(this, reg, listener);
		log.debug("Object {0}: Added listener of {1}", getId(), eventType);
		return reg;
	}

	@Override
	public void removeListener(ListenerRegistration listenerRegistration) {
		if (listenerRegistration instanceof ErrorListenerRegistration) {
			removeListener((ErrorListenerRegistration) listenerRegistration);
		} else if (listenerRegistration instanceof EventListenerRegistration) {
			removeListener((EventListenerRegistration) listenerRegistration);
		}
	}

	private void removeListener(ErrorListenerRegistration listenerRegistration) {
		Client client = clientPool.acquireSync();
		try {
			client.unsubscribeEvent(objectRef.getThriftRef(),
					listenerRegistration.getRegistrationId());
		} catch (KmsMediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}
		handler.removeErrorListener(this, listenerRegistration);
		log.debug("Object {0}: Removed listener {1}", getId(),
				listenerRegistration.getRegistrationId());
	}

	private void removeListener(EventListenerRegistration listenerRegistration) {
		Client client = clientPool.acquireSync();
		try {
			client.unsubscribeEvent(objectRef.getThriftRef(),
					listenerRegistration.getRegistrationId());
		} catch (KmsMediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}
		handler.removeListener(this, listenerRegistration);
		log.debug("Object {0}: Removed listener {1}", getId(),
				listenerRegistration.getRegistrationId());
	}

	@Override
	public ListenerRegistration addErrorListener(MediaErrorListener listener) {
		Client client = clientPool.acquireSync();

		String callbackToken;

		try {
			// TODO change for real call
			callbackToken = client.subscribeError(objectRef.getThriftRef(),
					config.getHandlerAddress(), config.getHandlerPort());
		} catch (KmsMediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}

		ErrorListenerRegistration reg = new ErrorListenerRegistration(
				callbackToken);
		handler.addErrorListener(this, reg, listener);
		log.debug("Object {0}: Added error listener", getId());
		return reg;
	}

	/**
	 * Invoke a remote method in this media object. This overlaod is used for
	 * methods that receive no arguments.
	 * 
	 * @param method
	 *            The name of the method
	 * @return Return of the invoked method
	 */
	protected MediaParam invoke(String method) {
		return invoke(method, new HashMap<String, MediaParam>(0));
	}

	/**
	 * Invoke a remote method in this media object. Internally, the method
	 * {@link AbstractMediaObject#invoke(String)} calls this method with an
	 * empty map.
	 * 
	 * @param method
	 *            The name of the method
	 * @param params
	 *            Parameters passed as argument to the remote method.
	 * @return Return of the invoked method
	 */
	protected MediaParam invoke(String method, Map<String, MediaParam> params) {
		Client client = clientPool.acquireSync();

		KmsMediaParam result;

		try {
			result = client.invoke(objectRef.getThriftRef(), method,
					transformMediaParamsMap(params));
		} catch (KmsMediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}

		log.debug("Object {0}: Successfully invoked method {1}", getId(),
				method);
		return (AbstractMediaParam) ctx.getBean("mediaParam", result);
	}

	@Override
	public MediaObject getParent() {

		if (parent == null) {
			Client client = clientPool.acquireSync();

			MediaObjectRef objRefDTO;

			try {
				objRefDTO = fromThrift(client.getParent(objectRef
						.getThriftRef()));
			} catch (KmsMediaServerException e) {
				throw new KurentoMediaFrameworkException(e.getMessage(), e,
						e.getErrorCode());
			} catch (TException e) {
				// TODO change error code
				throw new KurentoMediaFrameworkException(e.getMessage(), e,
						30000);
			} finally {
				clientPool.release(client);
			}

			parent = (MediaObject) ctx.getBean("mediaObject", objRefDTO);
		}

		return parent;
	}

	@Override
	public MediaPipeline getMediaPipeline() {

		if (pipeline == null) {
			Client client = clientPool.acquireSync();

			MediaPipelineRef pipelineRefDTO;
			try {
				pipelineRefDTO = new MediaPipelineRef(
						client.getMediaPipeline(objectRef.getThriftRef()));
			} catch (KmsMediaServerException e) {
				throw new KurentoMediaFrameworkException(e.getMessage(), e,
						e.getErrorCode());
			} catch (TException e) {
				// TODO change error code
				throw new KurentoMediaFrameworkException(e.getMessage(), e,
						30000);
			} finally {
				clientPool.release(client);
			}

			pipeline = (MediaPipelineImpl) ctx.getBean("mediaObject",
					pipelineRefDTO);
		}

		return pipeline;
	}

	@Override
	public void release(final Continuation<Void> cont) {
		handler.removeAllListeners(this);

		final AsyncClient client = clientPool.acquireAsync();

		try {
			client.release(objectRef.getThriftRef(),
					new AsyncMethodCallback<AsyncClient.release_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							log.error("Object {0}: Async release error",
									getId());
							cont.onError(exception);
						}

						@Override
						public void onComplete(release_call response) {
							try {
								response.getResult();
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
							log.debug("Object {0}: Async released", getId());
							cont.onSuccess(null);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
	}

	@Override
	public <E extends MediaEvent> void addListener(final String eventType,
			final MediaEventListener<E> listener,
			final Continuation<ListenerRegistration> cont) {

		final AsyncClient client = clientPool.acquireAsync();

		try {

			client.subscribeEvent(this.objectRef.getThriftRef(), eventType,
					config.getHandlerAddress(), config.getHandlerPort(),
					new AsyncMethodCallback<subscribeEvent_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							log.error(
									"Object {0}: Async add listener error {1}",
									getId(), exception.getMessage());
							cont.onError(exception);
						}

						@Override
						public void onComplete(subscribeEvent_call response) {
							String token;
							try {
								token = response.getResult();
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

							EventListenerRegistration reg = new EventListenerRegistration(
									token);
							handler.addListener(AbstractMediaObject.this, reg,
									listener);
							log.debug(
									"Object {0}: Async. added listener for event {1}",
									getId(), eventType);
							cont.onSuccess(reg);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
	}

	@Override
	public void removeListener(final ListenerRegistration listenerRegistration,
			final Continuation<Void> cont) {
		if (listenerRegistration instanceof ErrorListenerRegistration) {
			removeListener((ErrorListenerRegistration) listenerRegistration,
					cont);
		} else if (listenerRegistration instanceof EventListenerRegistration) {
			removeListener((EventListenerRegistration) listenerRegistration,
					cont);
		}
	}

	private void removeListener(
			final EventListenerRegistration listenerRegistration,
			final Continuation<Void> cont) {
		// TODO add error removal
		final AsyncClient client = clientPool.acquireAsync();

		try {
			client.unsubscribeEvent(objectRef.getThriftRef(),
					listenerRegistration.getRegistrationId(),
					new AsyncMethodCallback<unsubscribeEvent_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							log.error("Object {0}: Async. remove listener {1}",
									getId(),
									listenerRegistration.getRegistrationId());
							cont.onError(exception);
						}

						@Override
						public void onComplete(unsubscribeEvent_call response) {
							try {
								response.getResult();
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

							handler.removeListener(AbstractMediaObject.this,
									listenerRegistration);
							log.debug(
									"Object {0}: Async. removed listener {1}",
									getId(),
									listenerRegistration.getRegistrationId());
							cont.onSuccess(null);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
	}

	private void removeListener(
			final ErrorListenerRegistration listenerRegistration,
			final Continuation<Void> cont) {
		// TODO add error removal
		final AsyncClient client = clientPool.acquireAsync();

		try {
			client.unsubscribeError(objectRef.getThriftRef(),
					listenerRegistration.getRegistrationId(),
					new AsyncMethodCallback<unsubscribeError_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							log.error("Object {0}: Async. remove listener {1}",
									getId(),
									listenerRegistration.getRegistrationId());
							cont.onError(exception);
						}

						@Override
						public void onComplete(unsubscribeError_call response) {
							try {
								response.getResult();
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

							handler.removeErrorListener(
									AbstractMediaObject.this,
									listenerRegistration);
							log.debug(
									"Object {0}: Async. removed listener {1}",
									getId(),
									listenerRegistration.getRegistrationId());
							cont.onSuccess(null);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
	}

	@Override
	public void addErrorListener(final MediaErrorListener listener,
			final Continuation<ListenerRegistration> cont) {

		final AsyncClient client = clientPool.acquireAsync();

		try {
			// TODO use real call to subscribeToError
			client.subscribeError(this.objectRef.getThriftRef(),
					config.getHandlerAddress(), config.getHandlerPort(),
					new AsyncMethodCallback<subscribeError_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							log.error(
									"Object {0}: Async add listener error {1}",
									getId(), exception.getMessage());
							cont.onError(exception);
						}

						@Override
						public void onComplete(subscribeError_call response) {
							String token;
							try {
								token = response.getResult();
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

							ErrorListenerRegistration reg = new ErrorListenerRegistration(
									token);
							handler.addErrorListener(AbstractMediaObject.this,
									reg, listener);
							log.debug(
									"Object {0}: Async. added error listener",
									getId());
							cont.onSuccess(reg);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
	}

	/**
	 * Invoke a remote method in this media object. This overlaod is used for
	 * methods that receive no arguments.
	 * 
	 * @param method
	 *            The name of the method
	 */
	protected <R extends MediaParam> void invoke(final String method,
			final Continuation<R> cont) {
		invoke(method, new HashMap<String, MediaParam>(0), cont);
	}

	/**
	 * Invoke a remote method in this media object. Internally, the method
	 * {@link AbstractMediaObject#invoke(String, Continuation)} calls this
	 * method with an empty map.
	 * 
	 * @param method
	 *            The name of the method
	 * @param params
	 *            Parameters passed as argument to the remote method.
	 */
	protected <R extends MediaParam> void invoke(final String method,
			final Map<String, MediaParam> params, final Continuation<R> cont) {
		final AsyncClient client = this.clientPool.acquireAsync();

		try {
			client.invoke(this.objectRef.getThriftRef(), method,
					transformMediaParamsMap(params),
					new AsyncMethodCallback<invoke_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							log.error("Object {0}: Async. invocation {1}",
									getId(), method);
							cont.onError(exception);
						}

						@Override
						public void onComplete(invoke_call response) {
							KmsMediaParam result;

							try {
								result = response.getResult();
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
							// This casting should always return the expected
							// MediaParam child.
							@SuppressWarnings("unchecked")
							R mediaResult = (R) ctx.getBean("mediaParam",
									result);
							log.debug(
									"Object {0}: Async. successfully invoked method {1}",
									getId(), method);
							cont.onSuccess(mediaResult);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	public <F extends MediaObject> void getParent(final Continuation<F> cont) {

		if (parent == null) {
			final AsyncClient client = this.clientPool.acquireAsync();

			try {
				client.getParent(this.objectRef.getThriftRef(),
						new AsyncMethodCallback<AsyncClient.getParent_call>() {

							@Override
							public void onError(Exception exception) {
								clientPool.release(client);
								cont.onError(exception);
							}

							@Override
							public void onComplete(getParent_call response) {
								MediaObjectRef refDTO;

								try {
									refDTO = fromThrift(response.getResult());
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

								// TODO check if this cast is ok
								parent = (F) ctx.getBean("mediaObject", refDTO);
								cont.onSuccess((F) parent);
							}
						});
			} catch (TException e) {
				clientPool.release(client);
				// TODO change error code
				throw new KurentoMediaFrameworkException(e.getMessage(), e,
						30000);
			}
		} else {
			// TODO check if this cast is ok
			cont.onSuccess((F) parent);
		}
	}

	@Override
	public void getMediaPipeline(final Continuation<MediaPipeline> cont) {
		if (pipeline == null) {
			final AsyncClient client = this.clientPool.acquireAsync();

			try {
				client.getMediaPipeline(
						this.objectRef.getThriftRef(),
						new AsyncMethodCallback<AsyncClient.getMediaPipeline_call>() {

							@Override
							public void onError(Exception exception) {
								clientPool.release(client);
								cont.onError(exception);
							}

							@Override
							public void onComplete(
									getMediaPipeline_call response) {
								MediaPipelineRef objRef;

								try {
									objRef = new MediaPipelineRef(response
											.getResult());
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

								pipeline = (MediaPipelineImpl) ctx.getBean(
										"mediaObject", objRef);
								cont.onSuccess(pipeline);
							}
						});
			} catch (TException e) {
				clientPool.release(client);
				// TODO change error code
				throw new KurentoMediaFrameworkException(e.getMessage(), e,
						30000);
			}
		} else {
			cont.onSuccess(pipeline);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (this == obj) {
			return true;
		}

		if (!obj.getClass().equals(this.getClass())) {
			return false;
		}

		AbstractMediaObject mo = (AbstractMediaObject) obj;
		return mo.objectRef.equals(this.objectRef);
	}

	@Override
	public int hashCode() {
		return this.objectRef.hashCode();
	}

	protected Map<String, KmsMediaParam> transformMediaParamsMap(
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

	// TODO think about MediaElement here...
	protected static abstract class AbstractMediaObjectBuilder<T extends AbstractMediaObjectBuilder<T, E>, E extends MediaObject> {

		protected final Map<String, MediaParam> params = new HashMap<String, MediaParam>();

		protected final String elementName;

		protected final MediaPipeline pipeline;

		protected AbstractMediaObjectBuilder(final String elementName,
				final MediaPipeline pipeline) {
			this.elementName = elementName;
			this.pipeline = pipeline;
		}

		@SuppressWarnings("unchecked")
		protected final T self() {
			return (T) this;
		}

		public abstract E build();

		public abstract void buildAsync(Continuation<E> cont);

	}

}
