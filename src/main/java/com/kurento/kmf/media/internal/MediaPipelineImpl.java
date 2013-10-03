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
import com.kurento.kms.thrift.api.KmsMediaObjectRef;
import com.kurento.kms.thrift.api.KmsMediaParam;
import com.kurento.kms.thrift.api.KmsMediaPlayerEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaRecorderEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaRtpEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaServerException;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.connectElements_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.createMediaElementWithParams_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.createMediaElement_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.createMediaMixerWithParams_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.createMediaMixer_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.Client;
import com.kurento.kms.thrift.api.KmsMediaUriEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaWebRtcEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaZBarFilterTypeConstants;

public class MediaPipelineImpl extends AbstractMediaObject implements
		MediaPipeline {

	public MediaPipelineImpl(MediaPipelineRef objectRef) {
		super(objectRef);
	}

	@Override
	public void connect(MediaElement source, MediaElement sink) {
		Client client = clientPool.acquireSync();

		KmsMediaObjectRef sinkRef = ((AbstractMediaObject) sink).getObjectRef()
				.getThriftRef();
		KmsMediaObjectRef srcRef = ((AbstractMediaObject) source)
				.getObjectRef().getThriftRef();
		try {
			client.connectElements(srcRef, sinkRef);
		} catch (KmsMediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			this.clientPool.release(client);
		}

	}

	@Override
	public void connect(MediaElement source, MediaElement sink,
			final Continuation<Void> cont) {
		final AsyncClient client = clientPool.acquireAsync();
		try {
			KmsMediaObjectRef sinkRef = ((AbstractMediaObject) sink)
					.getObjectRef().getThriftRef();
			KmsMediaObjectRef srcRef = ((AbstractMediaObject) source)
					.getObjectRef().getThriftRef();
			client.connectElements(srcRef, sinkRef,
					new AsyncMethodCallback<connectElements_call>() {

						@Override
						public void onComplete(connectElements_call response) {

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
								MediaPipelineImpl.this.clientPool
										.release(client);
							}
							cont.onSuccess(null);
						}

						@Override
						public void onError(Exception exception) {
							MediaPipelineImpl.this.clientPool.release(client);
							cont.onError(exception);
						}

					});
		} catch (TException e) {
			this.clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

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

		Client client = clientPool.acquireSync();
		MediaElementRef elementRefDTO;
		try {
			// TODO transform map
			elementRefDTO = new MediaElementRef(
					client.createMediaElementWithParams(null, "",
							new HashMap<String, KmsMediaParam>()));
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

		Client client = this.clientPool.acquireSync();
		MediaMixerRef mixerRefDTO;
		try {
			// TODO add params
			mixerRefDTO = new MediaMixerRef(client.createMediaMixerWithParams(
					this.objectRef.getThriftRef(), mixerType,
					new HashMap<String, KmsMediaParam>()));
		} catch (KmsMediaServerException e) {
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
			Map<String, MediaParam> params, final Continuation<T> cont) {

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

		final AsyncClient client = this.clientPool.acquireAsync();

		try {
			// TODO add params
			client.createMediaMixerWithParams(this.objectRef.getThriftRef(),
					mixerType, new HashMap<String, KmsMediaParam>(),
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
		return (HttpEndPoint) createMediaElement(KmsMediaHttpEndPointTypeConstants.TYPE_NAME);
	}

	@Override
	public HttpEndPoint createHttpEndPoint(int cookieLifetime,
			int disconnectionTimeout) {
		return createHttpEndPoint(cookieLifetime, disconnectionTimeout, false);
	}

	@Override
	public HttpEndPoint createHttpEndPoint(int cookieLifetime,
			int disconnectionTimeout, boolean excudeFromDGC) {
		return (HttpEndPoint) createMediaElement(
				KmsMediaHttpEndPointTypeConstants.TYPE_NAME,
				internalCreateHttpEndPointConstructorParams(null,
						cookieLifetime, disconnectionTimeout, excudeFromDGC));
	}

	private Map<String, MediaParam> internalCreateMediaObjectConstructorParams(
			Map<String, MediaParam> params, boolean excludeFromDGC) {
		if (params == null) {
			params = new HashMap<String, MediaParam>(1);
		}
		MediaObjectConstructorParam mocp = new MediaObjectConstructorParam();
		mocp.excludeFromGC = excludeFromDGC;
		params.put(KmsMediaObjectConstants.CONSTRUCTOR_PARAMS_DATA_TYPE, mocp);
		return params;
	}

	private Map<String, MediaParam> internalCreateHttpEndPointConstructorParams(
			Map<String, MediaParam> params, int cookieLifetime,
			int disconnectionTimeout, boolean excludeFromDGC) {
		if (params == null) {
			params = new HashMap<String, MediaParam>(2);
		}

		HttpEndpointConstructorParam hecp = new HttpEndpointConstructorParam();
		hecp.setCookieLifetime(cookieLifetime);
		hecp.setDisconnectionTimeout(disconnectionTimeout);
		params.put(
				KmsMediaHttpEndPointTypeConstants.CONSTRUCTOR_PARAMS_DATA_TYPE,
				hecp);

		return internalCreateMediaObjectConstructorParams(params,
				excludeFromDGC);
	}

	@Override
	public void createHttpEndPoint(int cookieLifetime,
			int disconnectionTimeout, Continuation<HttpEndPoint> cont) {
		createHttpEndPoint(cookieLifetime, disconnectionTimeout, false, cont);
	}

	@Override
	public void createHttpEndPoint(int cookieLifetime,
			int disconnectionTimeout, boolean excludeFromDGC,
			Continuation<HttpEndPoint> cont) {
		createMediaElement(
				KmsMediaHttpEndPointTypeConstants.TYPE_NAME,
				internalCreateHttpEndPointConstructorParams(null,
						cookieLifetime, disconnectionTimeout, excludeFromDGC),
				cont);
	}

	@Override
	public RtpEndPoint createRtpEndPoint() {
		return (RtpEndPoint) createMediaElement(KmsMediaRtpEndPointTypeConstants.TYPE_NAME);
	}

	@Override
	public RtpEndPoint createRtpEndPoint(boolean excludeFromDGC) {
		return (RtpEndPoint) createMediaElement(
				KmsMediaRtpEndPointTypeConstants.TYPE_NAME,
				internalCreateMediaObjectConstructorParams(null, excludeFromDGC));
	}

	@Override
	public void createRtpEndPoint(Continuation<RtpEndPoint> cont) {
		createMediaElement(KmsMediaRtpEndPointTypeConstants.TYPE_NAME, cont);
	}

	@Override
	public void createRtpEndPoint(boolean excludeFromDGC,
			Continuation<RtpEndPoint> cont) {
		createMediaElement(
				KmsMediaRtpEndPointTypeConstants.TYPE_NAME,
				internalCreateMediaObjectConstructorParams(null, excludeFromDGC),
				cont);
	}

	@Override
	public WebRtcEndPoint createWebRtcEndPoint() {
		return (WebRtcEndPoint) createMediaElement(KmsMediaWebRtcEndPointTypeConstants.TYPE_NAME);
	}

	@Override
	public WebRtcEndPoint createWebRtcEndPoint(boolean excludeFromDGC) {
		return (WebRtcEndPoint) createMediaElement(
				KmsMediaWebRtcEndPointTypeConstants.TYPE_NAME,
				internalCreateMediaObjectConstructorParams(null, excludeFromDGC));
	}

	@Override
	public void createWebRtcEndPoint(Continuation<WebRtcEndPoint> cont) {
		createMediaElement(KmsMediaWebRtcEndPointTypeConstants.TYPE_NAME);
	}

	@Override
	public void createWebRtcEndPoint(boolean excludeFromDGC,
			Continuation<WebRtcEndPoint> cont) {
		createMediaElement(
				KmsMediaWebRtcEndPointTypeConstants.TYPE_NAME,
				internalCreateMediaObjectConstructorParams(null, excludeFromDGC),
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
			Map<String, MediaParam> params, URI uri, boolean excludeFromDGC) {
		if (params == null) {
			params = new HashMap<String, MediaParam>(3);
		}
		UriEndPointConstructorParam param = new UriEndPointConstructorParam();
		param.setUri(uri);
		params.put(
				KmsMediaUriEndPointTypeConstants.CONSTRUCTOR_PARAMS_DATA_TYPE,
				param);
		return internalCreateMediaObjectConstructorParams(params,
				excludeFromDGC);

	}

	@Override
	public PlayerEndPoint createPlayerEndPoint(URI uri) {
		return (PlayerEndPoint) createMediaElement(
				KmsMediaPlayerEndPointTypeConstants.TYPE_NAME,
				internalCreateUriEndPointConstructorParams(null, uri, false));
	}

	@Override
	public PlayerEndPoint createPlayerEndPoint(String uriStr,
			boolean excludeFromDGC) {
		URI uri;
		try {
			uri = new URI(uriStr);
		} catch (URISyntaxException e) {
			// TODO Add error code
			throw new KurentoMediaFrameworkException("", 30000);
		}
		return createPlayerEndPoint(uri, excludeFromDGC);
	}

	@Override
	public PlayerEndPoint createPlayerEndPoint(URI uri, boolean excludeFromDGC) {
		return (PlayerEndPoint) createMediaElement(
				KmsMediaPlayerEndPointTypeConstants.TYPE_NAME,
				internalCreateUriEndPointConstructorParams(null, uri,
						excludeFromDGC));
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
		createMediaElement(KmsMediaWebRtcEndPointTypeConstants.TYPE_NAME,
				internalCreateUriEndPointConstructorParams(null, uri, false),
				cont);
	}

	@Override
	public void createPlayerEndPoint(String uriStr, boolean excludeFromDGC,
			Continuation<PlayerEndPoint> cont) {
		URI uri;
		try {
			uri = new URI(uriStr);
		} catch (URISyntaxException e) {
			// TODO Add error code
			throw new KurentoMediaFrameworkException("", 30000);
		}
		createPlayerEndPoint(uri, excludeFromDGC, cont);
	}

	@Override
	public void createPlayerEndPoint(URI uri, boolean excludeFromDGC,
			Continuation<PlayerEndPoint> cont) {
		createMediaElement(
				KmsMediaWebRtcEndPointTypeConstants.TYPE_NAME,
				internalCreateUriEndPointConstructorParams(null, uri,
						excludeFromDGC), cont);
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
				internalCreateUriEndPointConstructorParams(null, uri, false));
	}

	@Override
	public RecorderEndPoint createRecorderEndPoint(String uriStr,
			boolean excludeFromDGC) {
		URI uri;
		try {
			uri = new URI(uriStr);
		} catch (URISyntaxException e) {
			// TODO Add error code
			throw new KurentoMediaFrameworkException("", 30000);
		}
		return createRecorderEndPoint(uri, excludeFromDGC);
	}

	@Override
	public RecorderEndPoint createRecorderEndPoint(URI uri,
			boolean excludeFromDGC) {
		return (RecorderEndPoint) createMediaElement(
				KmsMediaRecorderEndPointTypeConstants.TYPE_NAME,
				internalCreateUriEndPointConstructorParams(null, uri,
						excludeFromDGC));
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
		createMediaElement(KmsMediaRecorderEndPointTypeConstants.TYPE_NAME,
				internalCreateUriEndPointConstructorParams(null, uri, false),
				cont);
	}

	@Override
	public void createRecorderEndPoint(String uriStr, boolean excludeFromDGC,
			Continuation<RecorderEndPoint> cont) {
		URI uri;
		try {
			uri = new URI(uriStr);
		} catch (URISyntaxException e) {
			// TODO Add error code
			throw new KurentoMediaFrameworkException("", 30000);
		}
		createRecorderEndPoint(uri, excludeFromDGC, cont);
	}

	@Override
	public void createRecorderEndPoint(URI uri, boolean excludeFromDGC,
			Continuation<RecorderEndPoint> cont) {
		createMediaElement(
				KmsMediaRecorderEndPointTypeConstants.TYPE_NAME,
				internalCreateUriEndPointConstructorParams(null, uri,
						excludeFromDGC), cont);
	}

	@Override
	public ZBarFilter createZBarFilter() {
		return (ZBarFilter) createMediaElement(KmsMediaZBarFilterTypeConstants.TYPE_NAME);
	}

	@Override
	public ZBarFilter createZBarFilter(boolean excludeFromDGC) {
		return (ZBarFilter) createMediaElement(
				KmsMediaZBarFilterTypeConstants.TYPE_NAME,
				internalCreateMediaObjectConstructorParams(null, excludeFromDGC));
	}

	@Override
	public void createZBarFilter(Continuation<ZBarFilter> cont) {
		createMediaElement(KmsMediaZBarFilterTypeConstants.TYPE_NAME, cont);
	}

	@Override
	public void createZBarFilter(boolean excludeFromDGC,
			Continuation<ZBarFilter> cont) {
		createMediaElement(
				KmsMediaZBarFilterTypeConstants.TYPE_NAME,
				internalCreateMediaObjectConstructorParams(null, excludeFromDGC),
				cont);
	}

	@Override
	public JackVaderFilter createJackVaderFilter() {
		return (JackVaderFilter) createMediaElement(KmsMediaJackVaderFilterTypeConstants.TYPE_NAME);
	}

	@Override
	public JackVaderFilter createJackVaderFilter(boolean excludeFromDGC) {
		return (JackVaderFilter) createMediaElement(
				KmsMediaJackVaderFilterTypeConstants.TYPE_NAME,
				internalCreateMediaObjectConstructorParams(null, excludeFromDGC));
	}

	@Override
	public void createJackVaderFilter(Continuation<JackVaderFilter> cont) {
		createMediaElement(KmsMediaJackVaderFilterTypeConstants.TYPE_NAME, cont);
	}

	@Override
	public void createJackVaderFilter(boolean excludeFromDGC,
			Continuation<JackVaderFilter> cont) {

		createMediaElement(
				KmsMediaJackVaderFilterTypeConstants.TYPE_NAME,
				internalCreateMediaObjectConstructorParams(null, excludeFromDGC),
				cont);
	}
}
