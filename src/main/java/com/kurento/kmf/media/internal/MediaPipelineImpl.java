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
import com.kurento.kmf.media.PlayerEndPoint;
import com.kurento.kmf.media.RecorderEndPoint;
import com.kurento.kmf.media.RtpEndPoint;
import com.kurento.kmf.media.WebRtcEndPoint;
import com.kurento.kmf.media.ZBarFilter;
import com.kurento.kmf.media.commands.MediaParams;
import com.kurento.kmf.media.commands.internal.AbstractMediaParams;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;
import com.kurento.kmf.media.internal.refs.MediaMixerRefDTO;
import com.kurento.kmf.media.internal.refs.MediaPipelineRefDTO;
import com.kurento.kms.thrift.api.HttpEndPointTypeConstants;
import com.kurento.kms.thrift.api.JackVaderFilterTypeConstants;
import com.kurento.kms.thrift.api.MediaServerException;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.createMediaElementWithParams_call;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.createMediaElement_call;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.createMediaMixerWithParams_call;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.createMediaMixer_call;
import com.kurento.kms.thrift.api.MediaServerService.Client;
import com.kurento.kms.thrift.api.PlayerEndPointTypeConstants;
import com.kurento.kms.thrift.api.RecorderEndPointTypeConstants;
import com.kurento.kms.thrift.api.RtpEndPointTypeConstants;
import com.kurento.kms.thrift.api.WebRtcEndPointTypeConstants;
import com.kurento.kms.thrift.api.ZBarFilterTypeConstants;

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
	public MediaElement createMediaElement(String elementType,
			MediaParams params) {

		Client client = clientPool.acquireSync();
		MediaElementRefDTO elementRefDTO;
		try {
			elementRefDTO = new MediaElementRefDTO(
					client.createMediaElementWithParams(
							this.objectRef.getThriftRef(), elementType,
							((AbstractMediaParams) params).getThriftParams()));
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
	public MediaMixer createMediaMixer(String mixerType, MediaParams params) {

		Client client = this.clientPool.acquireSync();
		MediaMixerRefDTO mixerRefDTO;
		try {
			mixerRefDTO = new MediaMixerRefDTO(
					client.createMediaMixerWithParams(
							this.objectRef.getThriftRef(), mixerType,
							((AbstractMediaParams) params).getThriftParams()));
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
			MediaParams params, final Continuation<T> cont) {

		final AsyncClient client = this.clientPool.acquireAsync();
		try {
			client.createMediaElementWithParams(
					this.objectRef.getThriftRef(),
					elementType,
					((AbstractMediaParams) params).getThriftParams(),
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
			MediaParams params, final Continuation<T> cont) {

		final AsyncClient client = this.clientPool.acquireAsync();

		try {
			client.createMediaMixerWithParams(this.objectRef.getThriftRef(),
					mixerType,
					((AbstractMediaParams) params).getThriftParams(),
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
		return (HttpEndPoint) createMediaElement(HttpEndPointTypeConstants.TYPE_NAME);
	}

	@Override
	public RtpEndPoint createRtpEndPoint() {
		return (RtpEndPoint) createMediaElement(RtpEndPointTypeConstants.TYPE_NAME);
	}

	@Override
	public WebRtcEndPoint createWebRtcEndPoint() {
		return (WebRtcEndPoint) createMediaElement(WebRtcEndPointTypeConstants.TYPE_NAME);
	}

	@Override
	public PlayerEndPoint createPlayerEndPoint(String uri) {
		MediaParams params = null;// TODO new StringMediaParams(uri);
		return (PlayerEndPoint) createMediaElement(
				PlayerEndPointTypeConstants.TYPE_NAME, params);
	}

	@Override
	public RecorderEndPoint createRecorderEndPoint(String uri) {
		MediaParams params = null; // TODO new StringMediaParams(uri);
		return (RecorderEndPoint) createMediaElement(
				RecorderEndPointTypeConstants.TYPE_NAME, params);
	}

	@Override
	public ZBarFilter createZBarFilter() {
		return (ZBarFilter) createMediaElement(ZBarFilterTypeConstants.TYPE_NAME);
	}

	@Override
	public JackVaderFilter createJackVaderFilter() {
		return (JackVaderFilter) createMediaElement(JackVaderFilterTypeConstants.TYPE_NAME);
	}

	@Override
	public void createHttpEndPoint(Continuation<HttpEndPoint> cont) {
		createMediaElement(HttpEndPointTypeConstants.TYPE_NAME, cont);
	}

	@Override
	public void createRtpEndPoint(Continuation<RtpEndPoint> cont) {
		createMediaElement(RtpEndPointTypeConstants.TYPE_NAME, cont);

	}

	@Override
	public void createWebRtcEndPoint(Continuation<WebRtcEndPoint> cont) {
		createMediaElement(WebRtcEndPointTypeConstants.TYPE_NAME, cont);

	}

	@Override
	public void createPlayerEndPoint(String uri,
			Continuation<PlayerEndPoint> cont) {
		MediaParams params = null; // TODO new StringMediaParams(uri);
		createMediaElement(PlayerEndPointTypeConstants.TYPE_NAME, params, cont);
	}

	@Override
	public void createRecorderEndPoint(String uri,
			Continuation<RecorderEndPoint> cont) {
		MediaParams params = null; // TODO new StringMediaParams(uri);
		createMediaElement(RecorderEndPointTypeConstants.TYPE_NAME, params,
				cont);

	}

	@Override
	public void createZBarFilter(Continuation<ZBarFilter> cont) {
		createMediaElement(ZBarFilterTypeConstants.TYPE_NAME, cont);
	}

	@Override
	public void createJackVaderFilter(Continuation<JackVaderFilter> cont) {
		createMediaElement(JackVaderFilterTypeConstants.TYPE_NAME, cont);
	}
}
