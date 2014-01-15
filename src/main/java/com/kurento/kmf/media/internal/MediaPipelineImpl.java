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
import java.util.Map;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.ChromaFilter.ChromaFilterBuilder;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.FaceOverlayFilter.FaceOverlayFilterBuilder;
import com.kurento.kmf.media.GStreamerFilter.GStreamerFilterBuilder;
import com.kurento.kmf.media.HttpGetEndpoint.HttpGetEndpointBuilder;
import com.kurento.kmf.media.HttpPostEndpoint.HttpPostEndpointBuilder;
import com.kurento.kmf.media.JackVaderFilter.JackVaderFilterBuilder;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaMixer;
import com.kurento.kmf.media.MediaObject;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaType;
import com.kurento.kmf.media.PlateDetectorFilter.PlateDetectorFilterBuilder;
import com.kurento.kmf.media.PlayerEndpoint.PlayerEndpointBuilder;
import com.kurento.kmf.media.PointerDetectorFilter.PointerDetectorFilterBuilder;
import com.kurento.kmf.media.RecorderEndpoint.RecorderEndpointBuilder;
import com.kurento.kmf.media.RtpEndpoint.RtpEndpointBuilder;
import com.kurento.kmf.media.WebRtcEndpoint.WebRtcEndpointBuilder;
import com.kurento.kmf.media.ZBarFilter.ZBarFilterBuilder;
import com.kurento.kmf.media.internal.ChromaFilterImpl.ChromaFilterBuilderImpl;
import com.kurento.kmf.media.internal.FaceOverlayFilterImpl.FaceOverlayFilterBuilderImpl;
import com.kurento.kmf.media.internal.GStreamerFilterImpl.GStreamerFilterBuilderImpl;
import com.kurento.kmf.media.internal.HttpGetEndpointImpl.HttpGetEndpointBuilderImpl;
import com.kurento.kmf.media.internal.HttpPostEndpointImpl.HttpPostEndpointBuilderImpl;
import com.kurento.kmf.media.internal.JackVaderFilterImpl.JackVaderFilterBuilderImpl;
import com.kurento.kmf.media.internal.PlateDetectorFilterImpl.PlateDetectorFilterBuilderImpl;
import com.kurento.kmf.media.internal.PlayerEndpointImpl.PlayerEndpointBuilderImpl;
import com.kurento.kmf.media.internal.PointerDetectorFilterImpl.PointerDetectorFilterBuilderImpl;
import com.kurento.kmf.media.internal.RecorderEndpointImpl.RecorderEndpointBuilderImpl;
import com.kurento.kmf.media.internal.RtpEndpointImpl.RtpEndpointBuilderImpl;
import com.kurento.kmf.media.internal.WebRtcEndpointImpl.WebRtcEndpointBuilderImpl;
import com.kurento.kmf.media.internal.ZBarFilterImpl.ZBarFilterBuilderImpl;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.internal.refs.MediaMixerRef;
import com.kurento.kmf.media.internal.refs.MediaPipelineRef;
import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kmf.media.params.internal.WindowParam;
import com.kurento.kms.thrift.api.KmsMediaServerException;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.createMediaElementWithParams_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.createMediaElement_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.createMediaMixerWithParams_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.createMediaMixer_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.Client;

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
			MediaType mediaType) {
		source.connect(sink, mediaType);
	}

	@Override
	public void connect(MediaElement source, MediaElement sink,
			MediaType mediaType, String mediaDescription) {
		source.connect(sink, mediaType, mediaDescription);
	}

	@Override
	public void connect(MediaElement source, MediaElement sink,
			final Continuation<Void> cont) {
		source.connect(sink, cont);
	}

	@Override
	public void connect(MediaElement source, MediaElement sink,
			MediaType mediaType, Continuation<Void> cont) {
		source.connect(sink, mediaType, cont);
	}

	@Override
	public void connect(MediaElement source, MediaElement sink,
			MediaType mediaType, String mediaDescription,
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
	public HttpGetEndpointBuilder newHttpGetEndpoint() {
		return new HttpGetEndpointBuilderImpl(this);
	}

	@Override
	public HttpPostEndpointBuilder newHttpPostEndpoint() {
		return new HttpPostEndpointBuilderImpl(this);
	}

	@Override
	public RtpEndpointBuilder newRtpEndpoint() {
		return new RtpEndpointBuilderImpl(this);
	}

	@Override
	public WebRtcEndpointBuilder newWebRtcEndpoint() {
		return new WebRtcEndpointBuilderImpl(this);
	}

	@Override
	public FaceOverlayFilterBuilder newFaceOverlayFilter() {
		return new FaceOverlayFilterBuilderImpl(this);
	}

	@Override
	public GStreamerFilterBuilder newGStreamerFilter(String command) {
		return new GStreamerFilterBuilderImpl(this, command);
	}

	@Override
	public PlayerEndpointBuilder newPlayerEndpoint(String uriStr) {
		try {
			return this.newPlayerEndpoint(new URI(uriStr));
		} catch (URISyntaxException e) {
			// TODO error-code
			throw new KurentoMediaFrameworkException("", 30000);
		}
	}

	@Override
	public PlayerEndpointBuilder newPlayerEndpoint(URI uri) {
		return new PlayerEndpointBuilderImpl(uri, this);
	}

	@Override
	public RecorderEndpointBuilder newRecorderEndpoint(String uriStr) {
		try {
			return this.newRecorderEndpoint(new URI(uriStr));
		} catch (URISyntaxException e) {
			// TODO error-code
			throw new KurentoMediaFrameworkException("", 30000);
		}
	}

	@Override
	public RecorderEndpointBuilder newRecorderEndpoint(URI uri) {
		return new RecorderEndpointBuilderImpl(uri, this);
	}

	@Override
	public ZBarFilterBuilder newZBarFilter() {
		return new ZBarFilterBuilderImpl(this);
	}

	@Override
	public JackVaderFilterBuilder newJackVaderFilter() {
		return new JackVaderFilterBuilderImpl(this);
	}

	@Override
	public PointerDetectorFilterBuilder newPointerDetectorFilter() {
		return new PointerDetectorFilterBuilderImpl(this);
	}

	@Override
	public PlateDetectorFilterBuilder newPlateDetectorFilter() {
		return new PlateDetectorFilterBuilderImpl(this);
	}

	@Override
	public ChromaFilterBuilder newChromaFilter(WindowParam window) {
		return new ChromaFilterBuilderImpl(this, window);
	}
}
