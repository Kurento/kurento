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

import static com.kurento.kms.thrift.api.KmsMediaServerConstants.DEFAULT_GARBAGE_COLLECTOR_PERIOD;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

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
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.internal.refs.MediaMixerRef;
import com.kurento.kmf.media.internal.refs.MediaPipelineRef;
import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kmf.media.params.internal.HttpEndpointConstructorParam;
import com.kurento.kmf.media.params.internal.MediaObjectConstructorParam;
import com.kurento.kmf.media.params.internal.UriEndPointConstructorParam;
import com.kurento.kms.thrift.api.KmsMediaHttpEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaJackVaderFilterTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaObjectConstants;
import com.kurento.kms.thrift.api.KmsMediaPlayerEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaRecorderEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaRtpEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaServerException;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.createMediaElementWithParams_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.createMediaElement_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.createMediaMixerWithParams_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.createMediaMixer_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.Client;
import com.kurento.kms.thrift.api.KmsMediaType;
import com.kurento.kms.thrift.api.KmsMediaUriEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaWebRtcEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaZBarFilterTypeConstants;

public class MediaPipelineImpl extends AbstractCollectableMediaObject implements
		MediaPipeline {

	public MediaPipelineImpl(MediaPipelineRef objectRef) {
		super(objectRef);
	}

	public MediaPipelineImpl(MediaPipelineRef objectRef,
			Map<String, MediaParam> params) {
		super(objectRef, params);
	}

	@Override
	public void connect(MediaElement source, MediaElement sink) {
		source.connect(sink);
	}

	@Override
	public void connect(MediaElement source, MediaElement sink,
			KmsMediaType mediaType) {
		source.connect(sink, mediaType);
	}

	@Override
	public void connect(MediaElement source, MediaElement sink,
			KmsMediaType mediaType, String mediaDescription) {
		source.connect(sink, mediaType, mediaDescription);
	}

	@Override
	public void connect(MediaElement source, MediaElement sink,
			final Continuation<Void> cont) {
		source.connect(sink, cont);
	}

	@Override
	public void connect(MediaElement source, MediaElement sink,
			KmsMediaType mediaType, Continuation<Void> cont) {
		source.connect(sink, mediaType, cont);
	}

	@Override
	public void connect(MediaElement source, MediaElement sink,
			KmsMediaType mediaType, String mediaDescription,
			Continuation<Void> cont) {
		source.connect(sink, mediaType, mediaDescription, cont);
	}

	@Override
	public MediaElement createMediaElement(String elementType) {
		Client client = clientPool.acquireSync();

		MediaElementRef elementRefDTO;
		try {
			elementRefDTO = new MediaElementRef(client.createMediaElement(
					this.objectRef.getThriftRef(), elementType));
		} catch (KmsMediaServerException e) {
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
			Map<String, MediaParam> params) {
		MediaElement element;

		if (params == null || params.isEmpty()) {
			element = createMediaElement(elementType);
		} else {
			Client client = clientPool.acquireSync();
			MediaElementRef elementRefDTO;
			try {
				elementRefDTO = new MediaElementRef(
						client.createMediaElementWithParams(
								this.objectRef.getThriftRef(), elementType,
								transformMediaParamsMap(params)));
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

			element = (MediaElementImpl) ctx.getBean("mediaObjectWithParams",
					elementRefDTO, params);
		}
		return element;
	}

	@Override
	public MediaMixer createMediaMixer(String mixerType) {
		Client client = clientPool.acquireSync();

		MediaMixerRef mixerRefDTO;
		try {
			mixerRefDTO = new MediaMixerRef(client.createMediaMixer(
					this.objectRef.getThriftRef(), mixerType));
		} catch (KmsMediaServerException e) {
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
	public MediaMixer createMediaMixer(String mixerType,
			Map<String, MediaParam> params) {
		MediaMixer mixer;

		if (params == null || params.isEmpty()) {
			mixer = createMediaMixer(mixerType);
		} else {
			Client client = this.clientPool.acquireSync();
			MediaMixerRef mixerRefDTO;
			try {
				mixerRefDTO = new MediaMixerRef(
						client.createMediaMixerWithParams(
								this.objectRef.getThriftRef(), mixerType,
								transformMediaParamsMap(params)));
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

			mixer = (MediaMixer) ctx.getBean("mediaObjectWithParams",
					mixerRefDTO, params);
		}
		return mixer;
	}

	@Override
	public MediaPipeline getParent() {
		return null;
	}

	@Override
	public MediaPipeline getMediaPipeline() {
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
							MediaElementRef elementRefDTO;
							try {
								elementRefDTO = new MediaElementRef(response
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
			final Map<String, MediaParam> params, final Continuation<T> cont) {
		if (params == null || params.isEmpty()) {
			createMediaElement(elementType, cont);
		} else {
			final AsyncClient client = this.clientPool.acquireAsync();
			try {

				client.createMediaElementWithParams(
						this.objectRef.getThriftRef(),
						elementType,
						transformMediaParamsMap(params),
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
								MediaElementRef elementRefDTO;
								try {
									elementRefDTO = new MediaElementRef(
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

								MediaElementImpl element = (MediaElementImpl) ctx
										.getBean("mediaObjectWithParams",
												elementRefDTO, params);
								cont.onSuccess((T) element);
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
							MediaMixerRef mixerRefDTO;
							try {
								mixerRefDTO = new MediaMixerRef(response
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
			Map<String, MediaParam> params, final Continuation<T> cont) {
		if (params == null || params.isEmpty()) {
			createMediaMixer(mixerType, cont);
		} else {
			final AsyncClient client = this.clientPool.acquireAsync();

			try {
				client.createMediaMixerWithParams(
						this.objectRef.getThriftRef(),
						mixerType,
						transformMediaParamsMap(params),
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
								MediaMixerRef mixerRefDTO;
								try {
									mixerRefDTO = new MediaMixerRef(response
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
								MediaMixer mixer = (MediaMixer) ctx.getBean(
										"mediaObject", mixerRefDTO);
								cont.onSuccess((T) mixer);
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
		return (HttpEndPoint) createMediaElement(KmsMediaHttpEndPointTypeConstants.TYPE_NAME);
	}

	@Override
	public HttpEndPoint createHttpEndPoint(int disconnectionTimeout) {
		return createHttpEndPoint(disconnectionTimeout,
				DEFAULT_GARBAGE_COLLECTOR_PERIOD);
	}

	@Override
	public HttpEndPoint createHttpEndPoint(int disconnectionTimeout,
			int garbagePeriod) {
		return (HttpEndPoint) createMediaElement(
				KmsMediaHttpEndPointTypeConstants.TYPE_NAME,
				internalCreateHttpEndPointConstructorParams(null,
						disconnectionTimeout, garbagePeriod));
	}

	private Map<String, MediaParam> internalCreateMediaObjectConstructorParams(
			Map<String, MediaParam> params, int garbagePeriod) {
		if (params == null) {
			params = new HashMap<String, MediaParam>(4);
		}

		if (garbagePeriod != DEFAULT_GARBAGE_COLLECTOR_PERIOD) {
			MediaObjectConstructorParam mocp = new MediaObjectConstructorParam();
			mocp.setGarbageCollectorPeriod(garbagePeriod);
			params.put(KmsMediaObjectConstants.CONSTRUCTOR_PARAMS_DATA_TYPE,
					mocp);
		}

		return params;
	}

	private Map<String, MediaParam> internalCreateHttpEndPointConstructorParams(
			Map<String, MediaParam> params, int disconnectionTimeout,
			int garbagePeriod) {
		if (params == null) {
			params = new HashMap<String, MediaParam>(4);
		}

		HttpEndpointConstructorParam hecp = new HttpEndpointConstructorParam();
		hecp.setDisconnectionTimeout(Integer.valueOf(disconnectionTimeout));
		params.put(
				KmsMediaHttpEndPointTypeConstants.CONSTRUCTOR_PARAMS_DATA_TYPE,
				hecp);

		return internalCreateMediaObjectConstructorParams(params, garbagePeriod);
	}

	@Override
	public void createHttpEndPoint(Continuation<HttpEndPoint> cont) {
		createMediaElement(KmsMediaHttpEndPointTypeConstants.TYPE_NAME, cont);
	}

	@Override
	public void createHttpEndPoint(int disconnectionTimeout,
			Continuation<HttpEndPoint> cont) {
		createHttpEndPoint(disconnectionTimeout,
				DEFAULT_GARBAGE_COLLECTOR_PERIOD, cont);
	}

	@Override
	public void createHttpEndPoint(int disconnectionTimeout, int garbagePeriod,
			Continuation<HttpEndPoint> cont) {
		createMediaElement(
				KmsMediaHttpEndPointTypeConstants.TYPE_NAME,
				internalCreateHttpEndPointConstructorParams(null,
						disconnectionTimeout, garbagePeriod), cont);
	}

	@Override
	public RtpEndPoint createRtpEndPoint() {
		return (RtpEndPoint) createMediaElement(KmsMediaRtpEndPointTypeConstants.TYPE_NAME);
	}

	@Override
	public RtpEndPoint createRtpEndPoint(int garbagePeriod) {
		return (RtpEndPoint) createMediaElement(
				KmsMediaRtpEndPointTypeConstants.TYPE_NAME,
				internalCreateMediaObjectConstructorParams(null, garbagePeriod));
	}

	@Override
	public void createRtpEndPoint(Continuation<RtpEndPoint> cont) {
		createMediaElement(KmsMediaRtpEndPointTypeConstants.TYPE_NAME, cont);
	}

	@Override
	public void createRtpEndPoint(int garbagePeriod,
			Continuation<RtpEndPoint> cont) {
		createMediaElement(
				KmsMediaRtpEndPointTypeConstants.TYPE_NAME,
				internalCreateMediaObjectConstructorParams(null, garbagePeriod),
				cont);
	}

	@Override
	public WebRtcEndPoint createWebRtcEndPoint() {
		return (WebRtcEndPoint) createMediaElement(KmsMediaWebRtcEndPointTypeConstants.TYPE_NAME);
	}

	@Override
	public WebRtcEndPoint createWebRtcEndPoint(int garbagePeriod) {
		return (WebRtcEndPoint) createMediaElement(
				KmsMediaWebRtcEndPointTypeConstants.TYPE_NAME,
				internalCreateMediaObjectConstructorParams(null, garbagePeriod));
	}

	@Override
	public void createWebRtcEndPoint(Continuation<WebRtcEndPoint> cont) {
		createMediaElement(KmsMediaWebRtcEndPointTypeConstants.TYPE_NAME);
	}

	@Override
	public void createWebRtcEndPoint(int garbagePeriod,
			Continuation<WebRtcEndPoint> cont) {
		createMediaElement(
				KmsMediaWebRtcEndPointTypeConstants.TYPE_NAME,
				internalCreateMediaObjectConstructorParams(null, garbagePeriod),
				cont);
	}

	@Override
	public PlayerEndPoint createPlayerEndPoint(String uriStr) {
		URI uri;
		try {
			uri = new URI(uriStr);
		} catch (URISyntaxException e) {
			// TODO Add error code
			throw new KurentoMediaFrameworkException("", 30000);
		}
		return createPlayerEndPoint(uri);
	}

	private Map<String, MediaParam> internalCreateUriEndPointConstructorParams(
			Map<String, MediaParam> params, URI uri, int garbagePeriod) {
		if (params == null) {
			params = new HashMap<String, MediaParam>(4);
		}
		UriEndPointConstructorParam param = new UriEndPointConstructorParam();
		param.setUri(uri);
		params.put(
				KmsMediaUriEndPointTypeConstants.CONSTRUCTOR_PARAMS_DATA_TYPE,
				param);
		return internalCreateMediaObjectConstructorParams(params, garbagePeriod);
	}

	@Override
	public PlayerEndPoint createPlayerEndPoint(URI uri) {
		return (PlayerEndPoint) createMediaElement(
				KmsMediaPlayerEndPointTypeConstants.TYPE_NAME,
				internalCreateUriEndPointConstructorParams(null, uri,
						DEFAULT_GARBAGE_COLLECTOR_PERIOD));
	}

	@Override
	public PlayerEndPoint createPlayerEndPoint(String uriStr, int garbagePeriod) {
		URI uri;
		try {
			uri = new URI(uriStr);
		} catch (URISyntaxException e) {
			// TODO Add error code
			throw new KurentoMediaFrameworkException("", 30000);
		}
		return createPlayerEndPoint(uri, garbagePeriod);
	}

	@Override
	public PlayerEndPoint createPlayerEndPoint(URI uri, int garbagePeriod) {
		return (PlayerEndPoint) createMediaElement(
				KmsMediaPlayerEndPointTypeConstants.TYPE_NAME,
				internalCreateUriEndPointConstructorParams(null, uri,
						garbagePeriod));
	}

	@Override
	public void createPlayerEndPoint(String uriStr,
			Continuation<PlayerEndPoint> cont) {
		URI uri;
		try {
			uri = new URI(uriStr);
		} catch (URISyntaxException e) {
			// TODO Add error code
			throw new KurentoMediaFrameworkException("", 30000);
		}
		createPlayerEndPoint(uri, cont);
	}

	@Override
	public void createPlayerEndPoint(URI uri, Continuation<PlayerEndPoint> cont) {
		createMediaElement(
				KmsMediaPlayerEndPointTypeConstants.TYPE_NAME,
				internalCreateUriEndPointConstructorParams(null, uri,
						DEFAULT_GARBAGE_COLLECTOR_PERIOD), cont);
	}

	@Override
	public void createPlayerEndPoint(String uriStr, int garbagePeriod,
			Continuation<PlayerEndPoint> cont) {
		URI uri;
		try {
			uri = new URI(uriStr);
		} catch (URISyntaxException e) {
			// TODO Add error code
			throw new KurentoMediaFrameworkException("", 30000);
		}
		createPlayerEndPoint(uri, garbagePeriod, cont);
	}

	@Override
	public void createPlayerEndPoint(URI uri, int garbagePeriod,
			Continuation<PlayerEndPoint> cont) {
		createMediaElement(
				KmsMediaPlayerEndPointTypeConstants.TYPE_NAME,
				internalCreateUriEndPointConstructorParams(null, uri,
						garbagePeriod), cont);
	}

	@Override
	public RecorderEndPoint createRecorderEndPoint(String uriStr) {
		URI uri;
		try {
			uri = new URI(uriStr);
		} catch (URISyntaxException e) {
			// TODO Add error code
			throw new KurentoMediaFrameworkException("", 30000);
		}
		return createRecorderEndPoint(uri);
	}

	@Override
	public RecorderEndPoint createRecorderEndPoint(URI uri) {
		return (RecorderEndPoint) createMediaElement(
				KmsMediaRecorderEndPointTypeConstants.TYPE_NAME,
				internalCreateUriEndPointConstructorParams(null, uri,
						DEFAULT_GARBAGE_COLLECTOR_PERIOD));
	}

	@Override
	public RecorderEndPoint createRecorderEndPoint(String uriStr,
			int garbagePeriod) {
		URI uri;
		try {
			uri = new URI(uriStr);
		} catch (URISyntaxException e) {
			// TODO Add error code
			throw new KurentoMediaFrameworkException("", 30000);
		}
		return createRecorderEndPoint(uri, garbagePeriod);
	}

	@Override
	public RecorderEndPoint createRecorderEndPoint(URI uri, int garbagePeriod) {
		return (RecorderEndPoint) createMediaElement(
				KmsMediaRecorderEndPointTypeConstants.TYPE_NAME,
				internalCreateUriEndPointConstructorParams(null, uri,
						garbagePeriod));
	}

	@Override
	public void createRecorderEndPoint(String uriStr,
			Continuation<RecorderEndPoint> cont) {
		URI uri;
		try {
			uri = new URI(uriStr);
		} catch (URISyntaxException e) {
			// TODO Add error code
			throw new KurentoMediaFrameworkException("", 30000);
		}
		createRecorderEndPoint(uri, cont);
	}

	@Override
	public void createRecorderEndPoint(URI uri,
			Continuation<RecorderEndPoint> cont) {
		createMediaElement(
				KmsMediaRecorderEndPointTypeConstants.TYPE_NAME,
				internalCreateUriEndPointConstructorParams(null, uri,
						DEFAULT_GARBAGE_COLLECTOR_PERIOD), cont);
	}

	@Override
	public void createRecorderEndPoint(String uriStr, int garbagePeriod,
			Continuation<RecorderEndPoint> cont) {
		URI uri;
		try {
			uri = new URI(uriStr);
		} catch (URISyntaxException e) {
			// TODO Add error code
			throw new KurentoMediaFrameworkException("", 30000);
		}
		createRecorderEndPoint(uri, garbagePeriod, cont);
	}

	@Override
	public void createRecorderEndPoint(URI uri, int garbagePeriod,
			Continuation<RecorderEndPoint> cont) {
		createMediaElement(
				KmsMediaRecorderEndPointTypeConstants.TYPE_NAME,
				internalCreateUriEndPointConstructorParams(null, uri,
						garbagePeriod), cont);
	}

	@Override
	public ZBarFilter createZBarFilter() {
		return (ZBarFilter) createMediaElement(KmsMediaZBarFilterTypeConstants.TYPE_NAME);
	}

	@Override
	public ZBarFilter createZBarFilter(int garbagePeriod) {
		return (ZBarFilter) createMediaElement(
				KmsMediaZBarFilterTypeConstants.TYPE_NAME,
				internalCreateMediaObjectConstructorParams(null, garbagePeriod));
	}

	@Override
	public void createZBarFilter(Continuation<ZBarFilter> cont) {
		createMediaElement(KmsMediaZBarFilterTypeConstants.TYPE_NAME, cont);
	}

	@Override
	public void createZBarFilter(int garbagePeriod,
			Continuation<ZBarFilter> cont) {
		createMediaElement(
				KmsMediaZBarFilterTypeConstants.TYPE_NAME,
				internalCreateMediaObjectConstructorParams(null, garbagePeriod),
				cont);
	}

	@Override
	public JackVaderFilter createJackVaderFilter() {
		return (JackVaderFilter) createMediaElement(KmsMediaJackVaderFilterTypeConstants.TYPE_NAME);
	}

	@Override
	public JackVaderFilter createJackVaderFilter(int garbagePeriod) {
		return (JackVaderFilter) createMediaElement(
				KmsMediaJackVaderFilterTypeConstants.TYPE_NAME,
				internalCreateMediaObjectConstructorParams(null, garbagePeriod));
	}

	@Override
	public void createJackVaderFilter(Continuation<JackVaderFilter> cont) {
		createMediaElement(KmsMediaJackVaderFilterTypeConstants.TYPE_NAME, cont);
	}

	@Override
	public void createJackVaderFilter(int garbagePeriod,
			Continuation<JackVaderFilter> cont) {
		createMediaElement(
				KmsMediaJackVaderFilterTypeConstants.TYPE_NAME,
				internalCreateMediaObjectConstructorParams(null, garbagePeriod),
				cont);
	}

}
