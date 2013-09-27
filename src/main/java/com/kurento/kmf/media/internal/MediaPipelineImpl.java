package com.kurento.kmf.media.internal;

import java.util.Collection;
import java.util.HashMap;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.HttpEndPoint;
import com.kurento.kmf.media.JackVaderFilter;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaMixer;
import com.kurento.kmf.media.MediaObject;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaSink;
import com.kurento.kmf.media.MediaSource;
import com.kurento.kmf.media.PlayerEndPoint;
import com.kurento.kmf.media.RecorderEndPoint;
import com.kurento.kmf.media.RtpEndPoint;
import com.kurento.kmf.media.WebRtcEndPoint;
import com.kurento.kmf.media.ZBarFilter;
import com.kurento.kmf.media.commands.MediaParam;
import com.kurento.kmf.media.commands.internal.AbstractMediaCommand;
import com.kurento.kmf.media.commands.internal.SetUriCommand;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;
import com.kurento.kmf.media.internal.refs.MediaMixerRefDTO;
import com.kurento.kmf.media.internal.refs.MediaPipelineRefDTO;
import com.kurento.kms.thrift.api.MediaServerException;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.createMediaElementWithParams_call;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.createMediaElement_call;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.createMediaMixerWithParams_call;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.createMediaMixer_call;
import com.kurento.kms.thrift.api.MediaServerService.Client;
import com.kurento.kms.thrift.api.MediaType;
import com.kurento.kms.thrift.api.mediaServerConstants;

public class MediaPipelineImpl extends AbstractMediaObject implements
		MediaPipeline {

	public MediaPipelineImpl(MediaPipelineRefDTO objectRef) {
		super(objectRef);
	}

	@Override
	public MediaElement createMediaElement(String elementType) {
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

		MediaElementImpl element = (MediaElementImpl) ctx.getBean(
				"mediaObject", elementRefDTO);
		return element;
	}

	@Override
	public MediaElement createMediaElement(String elementType, MediaParam params) {

		Client client = clientPool.acquireSync();
		MediaElementRefDTO elementRefDTO;
		try {
			elementRefDTO = new MediaElementRefDTO(
					client.createMediaElementWithParams(
							this.objectRef.getThriftRef(), elementType,
							((AbstractMediaCommand) params).getThriftCommand()));
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}

		MediaElementImpl element = (MediaElementImpl) ctx.getBean(
				"mediaObject", elementRefDTO);
		return element;
	}

	@Override
	public MediaMixer createMediaMixer(String mixerType) {
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

	@Override
	public MediaMixer createMediaMixer(String mixerType, MediaParam params) {

		Client client = this.clientPool.acquireSync();
		MediaMixerRefDTO mixerRefDTO;
		try {
			mixerRefDTO = new MediaMixerRefDTO(
					client.createMediaMixerWithParams(
							this.objectRef.getThriftRef(), mixerType,
							((AbstractMediaCommand) params).getThriftCommand()));
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
	public MediaPipelineImpl getParent() throws KurentoMediaFrameworkException {
		return null;
	}

	@Override
	public MediaPipelineImpl getMediaPipeline() {
		return this;
	}

	@Override
	public <T extends MediaElement> void createMediaElement(String elementType,
			final Continuation<T> cont) {

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

						@SuppressWarnings("unchecked")
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

							MediaElementImpl element = (MediaElementImpl) ctx
									.getBean("mediaObject", elementRefDTO);
							cont.onSuccess((T) element);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

	@Override
	public <T extends MediaElement> void createMediaElement(String elementType,
			MediaParam params, final Continuation<T> cont) {

		final AsyncClient client = this.clientPool.acquireAsync();
		try {
			client.createMediaElementWithParams(
					this.objectRef.getThriftRef(),
					elementType,
					((AbstractMediaCommand) params).getThriftCommand(),
					new AsyncMethodCallback<createMediaElementWithParams_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@SuppressWarnings("unchecked")
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

							MediaElementImpl element = (MediaElementImpl) ctx
									.getBean("mediaObject", elementRefDTO);
							cont.onSuccess((T) element);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

	@Override
	public <T extends MediaMixer> void createMediaMixer(String mixerType,
			final Continuation<T> cont) {

		final AsyncClient client = this.clientPool.acquireAsync();

		try {
			client.createMediaMixer(this.objectRef.getThriftRef(), mixerType,
					new AsyncMethodCallback<createMediaMixer_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@SuppressWarnings("unchecked")
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
							cont.onSuccess((T) mixer);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

	@Override
	public <T extends MediaMixer> void createMediaMixer(String mixerType,
			MediaParam params, final Continuation<T> cont) {

		final AsyncClient client = this.clientPool.acquireAsync();

		try {
			client.createMediaMixerWithParams(this.objectRef.getThriftRef(),
					mixerType,
					((AbstractMediaCommand) params).getThriftCommand(),
					new AsyncMethodCallback<createMediaMixerWithParams_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@SuppressWarnings("unchecked")
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
							cont.onSuccess((T) mixer);
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
	public void getMediaPipeline(final Continuation<MediaPipeline> cont) {
		cont.onSuccess(this);
	}

	@Override
	public HttpEndPoint createHttpEndPoint() {
		return (HttpEndPoint) createMediaElement(mediaServerConstants.HTTP_END_POINT_TYPE);
	}

	@Override
	public RtpEndPoint createRtpEndPoint() {
		return (RtpEndPoint) createMediaElement(mediaServerConstants.RTP_END_POINT_TYPE);
	}

	@Override
	public WebRtcEndPoint createWebRtcEndPoint() {
		return (WebRtcEndPoint) createMediaElement(mediaServerConstants.WEB_RTP_END_POINT_TYPE);
	}

	@Override
	public PlayerEndPoint createPlayerEndPoint(String uri) {
		return (PlayerEndPoint) createMediaElement(
				mediaServerConstants.PLAYER_END_POINT_TYPE, new SetUriCommand(
						uri));
	}

	@Override
	public RecorderEndPoint createRecorderEndPoint(String uri) {
		return (RecorderEndPoint) createMediaElement(
				mediaServerConstants.RECORDER_END_POINT_TYPE,
				new SetUriCommand(uri));
	}

	@Override
	public ZBarFilter createZBarFilter() {
		return (ZBarFilter) createMediaElement(mediaServerConstants.ZBAR_FILTER_TYPE);
	}

	@Override
	public JackVaderFilter createJackVaderFilter() {
		return (JackVaderFilter) createMediaElement(mediaServerConstants.JACK_VADER_FILTER_TYPE);
	}

	@Override
	public void createHttpEndPoint(Continuation<HttpEndPoint> cont) {
		createMediaElement(mediaServerConstants.HTTP_END_POINT_TYPE, cont);
	}

	@Override
	public void createRtpEndPoint(Continuation<RtpEndPoint> cont) {
		createMediaElement(mediaServerConstants.RTP_END_POINT_TYPE, cont);

	}

	@Override
	public void createWebRtcEndPoint(Continuation<WebRtcEndPoint> cont) {
		createMediaElement(mediaServerConstants.WEB_RTP_END_POINT_TYPE, cont);

	}

	@Override
	public void createPlayerEndPoint(String uri,
			Continuation<PlayerEndPoint> cont) {
		createMediaElement(mediaServerConstants.PLAYER_END_POINT_TYPE,
				new SetUriCommand(uri), cont);
	}

	@Override
	public void createRecorderEndPoint(String uri,
			Continuation<RecorderEndPoint> cont) {
		createMediaElement(mediaServerConstants.RECORDER_END_POINT_TYPE,
				new SetUriCommand(uri), cont);

	}

	@Override
	public void createZBarFilter(Continuation<ZBarFilter> cont) {
		createMediaElement(mediaServerConstants.ZBAR_FILTER_TYPE, cont);
	}

	@Override
	public void createJackVaderFilter(Continuation<JackVaderFilter> cont) {
		createMediaElement(mediaServerConstants.JACK_VADER_FILTER_TYPE, cont);
	}
}
