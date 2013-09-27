package com.kurento.kmf.media.internal;

import static com.kurento.kmf.media.internal.refs.MediaRefConverter.fromThrift;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.ListenerRegistration;
import com.kurento.kmf.media.MediaApiConfiguration;
import com.kurento.kmf.media.MediaObject;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.commands.MediaCommand;
import com.kurento.kmf.media.commands.MediaCommandResult;
import com.kurento.kmf.media.commands.internal.AbstractMediaCommand;
import com.kurento.kmf.media.commands.internal.AbstractMediaCommandResult;
import com.kurento.kmf.media.events.MediaEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.internal.pool.MediaServerClientPoolService;
import com.kurento.kmf.media.internal.refs.MediaObjectRefDTO;
import com.kurento.kmf.media.internal.refs.MediaPipelineRefDTO;
import com.kurento.kms.thrift.api.CommandResult;
import com.kurento.kms.thrift.api.MediaServerException;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.getMediaPipeline_call;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.getParent_call;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.release_call;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.sendCommand_call;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.subscribe_call;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.unsubscribe_call;
import com.kurento.kms.thrift.api.MediaServerService.Client;

public abstract class AbstractMediaObject implements MediaObject {

	protected final MediaObjectRefDTO objectRef;

	@Autowired
	private DistributedGarbageCollector distributedGarbageCollector;

	@Autowired
	protected MediaServerClientPoolService clientPool;

	@Autowired
	private MediaServerCallbackHandler handler;

	@Autowired
	protected ApplicationContext ctx;

	@Autowired
	private MediaApiConfiguration config;

	private MediaPipelineImpl pipeline;

	private MediaObject parent;

	public MediaObjectRefDTO getObjectRef() {
		return objectRef;
	}

	public AbstractMediaObject(MediaObjectRefDTO ref) {
		objectRef = ref;
		distributedGarbageCollector.registerReference(objectRef.getThriftRef());
	}

	public Long getId() {
		return objectRef.getId();
	}

	@Override
	public void release() {

		distributedGarbageCollector.removeReference(objectRef.getThriftRef());
		handler.removeAllListeners(this);

		Client client = clientPool.acquireSync();

		try {
			client.release(this.objectRef.getThriftRef());
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}
	}

	@Override
	public <E extends MediaEvent> ListenerRegistration addListener(
			String eventType, MediaEventListener<E> listener) {
		Client client = clientPool.acquireSync();

		String callbackToken;

		try {
			callbackToken = client.subscribe(objectRef.getThriftRef(),
					eventType, config.getHandlerAddress(),
					config.getHandlerPort());
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}

		handler.addListener(this, new ListenerRegistrationImpl(callbackToken),
				listener);

		return new ListenerRegistrationImpl(callbackToken);
	}

	@Override
	public <E extends MediaEvent> void removeListener(
			ListenerRegistration listenerRegistration) {
		Client client = clientPool.acquireSync();

		try {
			client.unsubscribe(objectRef.getThriftRef(),
					listenerRegistration.getRegistrationId());
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}
		handler.removeListener(this, listenerRegistration);
	}

	protected MediaCommandResult sendCommand(MediaCommand command) {
		Client client = clientPool.acquireSync();

		CommandResult result;

		try {
			result = client.sendCommand(objectRef.getThriftRef(),
					((AbstractMediaCommand) command).getThriftCommand());
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}

		return (AbstractMediaCommandResult) ctx.getBean("mediaCommandResult",
				result);
	}

	@Override
	protected void finalize() {
		distributedGarbageCollector.removeReference(objectRef.getThriftRef());
		handler.removeAllListeners(this);
	}

	@Override
	public MediaObject getParent() {

		if (parent == null) {
			Client client = clientPool.acquireSync();

			MediaObjectRefDTO objRefDTO;

			try {
				objRefDTO = fromThrift(client.getParent(objectRef
						.getThriftRef()));
			} catch (MediaServerException e) {
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
	public MediaPipelineImpl getMediaPipeline() {

		if (pipeline == null) {
			Client client = clientPool.acquireSync();

			MediaPipelineRefDTO pipelineRefDTO;
			try {
				pipelineRefDTO = new MediaPipelineRefDTO(
						client.getMediaPipeline(objectRef.getThriftRef()));
			} catch (MediaServerException e) {
				throw new KurentoMediaFrameworkException(e.getMessage(), e,
						e.getErrorCode());
			} catch (TException e) {
				// TODO change error code
				throw new KurentoMediaFrameworkException(e.getMessage(), e,
						30000);
			} finally {
				clientPool.release(client);
			}

			pipeline = (MediaPipelineImpl) ctx.getBean("mediaPipeline",
					pipelineRefDTO);
		}

		return pipeline;
	}

	@Override
	public void release(final Continuation<Void> cont) {

		distributedGarbageCollector.removeReference(objectRef.getThriftRef());
		handler.removeAllListeners(this);

		final AsyncClient client = clientPool.acquireAsync();

		try {
			client.release(objectRef.getThriftRef(),
					new AsyncMethodCallback<AsyncClient.release_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@Override
						public void onComplete(release_call response) {
							try {
								response.getResult();
							} catch (MediaServerException e) {
								throw new KurentoMediaFrameworkException(e
										.getMessage(), e, e.getErrorCode());
							} catch (TException e) {
								// TODO change error code
								throw new KurentoMediaFrameworkException(e
										.getMessage(), e, 30000);
							} finally {
								clientPool.release(client);
							}
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
	public <E extends MediaEvent> void addListener(
			final MediaEventListener<E> listener, final String eventType,
			final Continuation<ListenerRegistration> cont) {

		final AsyncClient client = clientPool.acquireAsync();

		try {

			client.subscribe(this.objectRef.getThriftRef(), eventType,
					config.getHandlerAddress(), config.getHandlerPort(),
					new AsyncMethodCallback<subscribe_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@Override
						public void onComplete(subscribe_call response) {
							String token;
							try {
								token = response.getResult();
							} catch (MediaServerException e) {
								throw new KurentoMediaFrameworkException(e
										.getMessage(), e, e.getErrorCode());
							} catch (TException e) {
								// TODO change error code
								throw new KurentoMediaFrameworkException(e
										.getMessage(), e, 30000);
							} finally {
								clientPool.release(client);
							}
							handler.addListener(AbstractMediaObject.this,
									new ListenerRegistrationImpl(token),
									listener);
							cont.onSuccess(new ListenerRegistrationImpl(token));
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
	}

	@Override
	public <E extends MediaEvent> void removeListener(
			final ListenerRegistration listenerRegistration,
			final Continuation<Void> cont) {

		final AsyncClient client = clientPool.acquireAsync();

		try {
			client.unsubscribe(objectRef.getThriftRef(),
					listenerRegistration.getRegistrationId(),
					new AsyncMethodCallback<unsubscribe_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@Override
						public void onComplete(unsubscribe_call response) {
							try {
								response.getResult();
							} catch (MediaServerException e) {
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
							cont.onSuccess(null);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

	protected <R extends AbstractMediaCommandResult> void sendCommand(
			final MediaCommand command, final Continuation<R> cont) {
		final AsyncClient client = this.clientPool.acquireAsync();

		try {
			client.sendCommand(this.objectRef.getThriftRef(),
					((AbstractMediaCommand) command).getThriftCommand(),
					new AsyncMethodCallback<sendCommand_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@Override
						public void onComplete(sendCommand_call response) {
							CommandResult result;

							try {
								result = response.getResult();
							} catch (MediaServerException e) {
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
							// MediaCommandResult child.
							@SuppressWarnings("unchecked")
							R mediaResult = (R) ctx.getBean(
									"mediaCommandResult", result);
							cont.onSuccess(mediaResult);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

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
								MediaObjectRefDTO refDTO;

								try {
									refDTO = fromThrift(response.getResult());
								} catch (MediaServerException e) {
									throw new KurentoMediaFrameworkException(e
											.getMessage(), e, e.getErrorCode());
								} catch (TException e) {
									// TODO change error code
									throw new KurentoMediaFrameworkException(e
											.getMessage(), e, 30000);
								} finally {
									clientPool.release(client);
								}

								// FIXME parent in class is never set!
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
								MediaPipelineRefDTO objRef;

								try {
									objRef = new MediaPipelineRefDTO(response
											.getResult());
								} catch (MediaServerException e) {
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
										"mediaPipeline", objRef);
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
}
