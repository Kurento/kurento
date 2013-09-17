package com.kurento.kmf.media.objects;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;
import com.kurento.kmf.media.internal.refs.MediaMixerRefDTO;
import com.kurento.kmf.media.internal.refs.MediaPipelineRefDTO;
import com.kurento.kms.thrift.api.Command;
import com.kurento.kms.thrift.api.MediaServerException;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.createMediaElementWithParams_call;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.createMediaElement_call;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.createMediaMixerWithParams_call;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.createMediaMixer_call;
import com.kurento.kms.thrift.api.MediaServerService.Client;

public class MediaPipeline extends MediaObject {

	// TODO this is public cause it´s used in MediaApiAppContextConfiguration
	public MediaPipeline(MediaPipelineRefDTO objectRef) {
		super(objectRef);
	}

	public MediaElement createMediaElement(String elementType)
			throws KurentoMediaFrameworkException {
		Client client = clientPool.acquireSync();

		MediaElementRefDTO elementRefDTO;
		try {
			elementRefDTO = new MediaElementRefDTO(client.createMediaElement(
					this.objectRef.getThriftRef(), elementType));
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}

		MediaElement element = (MediaElement) ctx.getBean("mediaObject",
				elementRefDTO);
		return element;
	}

	public MediaElement createMediaElement(String elementType, Command params)
			throws KurentoMediaFrameworkException {
		// TODO change Command to Params
		Client client = clientPool.acquireSync();

		MediaElementRefDTO elementRefDTO;
		try {
			elementRefDTO = new MediaElementRefDTO(
					client.createMediaElementWithParams(
							this.objectRef.getThriftRef(), elementType, params));
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}

		MediaElement element = (MediaElement) ctx.getBean("mediaObject",
				elementRefDTO);
		return element;
	}

	public MediaMixer createMediaMixer(String mixerType)
			throws KurentoMediaFrameworkException {
		Client client = clientPool.acquireSync();

		MediaMixerRefDTO mixerRefDTO;
		try {
			mixerRefDTO = new MediaMixerRefDTO(client.createMediaMixer(
					this.objectRef.getThriftRef(), mixerType));
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}

		MediaMixer mixer = (MediaMixer) ctx.getBean("mediaObject", mixerRefDTO);
		return mixer;
	}

	protected MediaMixer createMediaMixer(String mixerType, Command params)
			throws KurentoMediaFrameworkException {
		Client client = this.clientPool.acquireSync();

		MediaMixerRefDTO mixerRefDTO;
		try {
			mixerRefDTO = new MediaMixerRefDTO(
					client.createMediaMixerWithParams(
							this.objectRef.getThriftRef(), mixerType, params));
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			this.clientPool.release(client);
		}

		MediaMixer mixer = (MediaMixer) ctx.getBean("mediaObject", mixerRefDTO);
		return mixer;
	}

	@Override
	public MediaPipeline getParent() throws KurentoMediaFrameworkException {
		return null;
	}

	@Override
	public MediaPipeline getMediaPipeline()
			throws KurentoMediaFrameworkException {
		return this;
	}

	public void createMediaElement(String elementType,
			final Continuation<MediaElement> cont)
			throws KurentoMediaFrameworkException {

		final AsyncClient client = this.clientPool.acquireAsync();

		try {
			client.createMediaElement(this.objectRef.getThriftRef(),
					elementType,
					new AsyncMethodCallback<createMediaElement_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@Override
						public void onComplete(createMediaElement_call response) {
							MediaElementRefDTO elementRefDTO;
							try {
								elementRefDTO = new MediaElementRefDTO(response
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

							MediaElement element = (MediaElement) ctx.getBean(
									"mediaObject", elementRefDTO);
							cont.onSuccess(element);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

	public void createMediaElement(String elementType, Command params,
			final Continuation<MediaElement> cont)
			throws KurentoMediaFrameworkException {

		// TODO change Command to Params
		final AsyncClient client = this.clientPool.acquireAsync();

		try {
			client.createMediaElementWithParams(
					this.objectRef.getThriftRef(),
					elementType,
					params,
					new AsyncMethodCallback<createMediaElementWithParams_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@Override
						public void onComplete(
								createMediaElementWithParams_call response) {
							MediaElementRefDTO elementRefDTO;
							try {
								elementRefDTO = new MediaElementRefDTO(response
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

							MediaElement element = (MediaElement) ctx.getBean(
									"mediaObject", elementRefDTO);
							cont.onSuccess(element);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

	public void createMediaMixer(String mixerType,
			final Continuation<MediaMixer> cont)
			throws KurentoMediaFrameworkException {

		final AsyncClient client = this.clientPool.acquireAsync();

		try {
			client.createMediaMixer(this.objectRef.getThriftRef(), mixerType,
					new AsyncMethodCallback<createMediaMixer_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@Override
						public void onComplete(createMediaMixer_call response) {
							MediaMixerRefDTO mixerRefDTO;
							try {
								mixerRefDTO = new MediaMixerRefDTO(response
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
							MediaMixer mixer = (MediaMixer) ctx.getBean(
									"mediaObject", mixerRefDTO);
							cont.onSuccess(mixer);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

	protected void createMediaMixer(String mixerType, Command params,
			final Continuation<MediaMixer> cont)
			throws KurentoMediaFrameworkException {

		final AsyncClient client = this.clientPool.acquireAsync();

		try {
			client.createMediaMixerWithParams(this.objectRef.getThriftRef(),
					mixerType, params,
					new AsyncMethodCallback<createMediaMixerWithParams_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@Override
						public void onComplete(
								createMediaMixerWithParams_call response) {
							MediaMixerRefDTO mixerRefDTO;
							try {
								mixerRefDTO = new MediaMixerRefDTO(response
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
							MediaMixer mixer = (MediaMixer) ctx.getBean(
									"mediaObject", mixerRefDTO);
							cont.onSuccess(mixer);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

	@Override
	public <F extends MediaObject> void getParent(final Continuation<F> cont)
			throws KurentoMediaFrameworkException {
		cont.onSuccess(null);
	}

	@Override
	public void getMediaPipeline(final Continuation<MediaPipeline> cont)
			throws KurentoMediaFrameworkException {
		cont.onSuccess(this);
	}

}
