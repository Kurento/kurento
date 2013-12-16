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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaSink;
import com.kurento.kmf.media.MediaSource;
import com.kurento.kmf.media.MediaType;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.internal.refs.MediaPadRef;
import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kmf.media.params.internal.MediaObjectConstructorParam;
import com.kurento.kms.thrift.api.KmsMediaObjectRef;
import com.kurento.kms.thrift.api.KmsMediaServerException;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.connectElementsByFullDescription_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.connectElementsByMediaType_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.connectElements_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.getMediaSinksByFullDescription_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.getMediaSinksByMediaType_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.getMediaSrcsByFullDescription_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.getMediaSrcsByMediaType_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.getMediaSrcs_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.Client;

public class MediaElementImpl extends AbstractCollectableMediaObject implements
		MediaElement {

	private static final Logger log = LoggerFactory
			.getLogger(MediaElementImpl.class);

	/**
	 * Constructor that configures, by default, the element as a non-collectable
	 * object: No keepalives will be sent to the media server, and the
	 * collection of this object by the JVM will NOT imply a destruction of the
	 * object in the server.
	 * 
	 * The object created by this means, will be associated with the life cycle
	 * of the enclosing {@link MediaPipeline}. In order to release the element,
	 * the user can invoke {@link MediaElement#release()}
	 * 
	 * @param objectRef
	 *            element reference
	 */
	public MediaElementImpl(MediaElementRef objectRef) {
		super(objectRef, 0);
	}

	/**
	 * Constructor with parameters to be sent to the media server. The entries
	 * in the map will be used by the server to configure the object while
	 * creating it.
	 * 
	 * If no garbage period is configured, using the structure
	 * {@link MediaObjectConstructorParam}, the object will NOT be collected in
	 * the media server, and will be associated with the life cycle of the
	 * enclosing {@link MediaPipeline}. In order to release the element, the
	 * user can invoke {@link MediaElement#release()}
	 * 
	 * @param objectRef
	 *            element reference
	 * @param params
	 *            map of parameters. The key is the name of the parameter, while
	 *            the value represents the param itself.
	 */
	public MediaElementImpl(MediaElementRef objectRef,
			Map<String, MediaParam> params) {
		super(objectRef, setDefaultGarbagePeriodParam(params, 0));
	}

	@Override
	public Collection<MediaSource> getMediaSrcs() {

		Client client = clientPool.acquireSync();

		List<KmsMediaObjectRef> srcRefs;

		try {
			srcRefs = client.getMediaSrcs(this.objectRef.getThriftRef());
		} catch (KmsMediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			this.clientPool.release(client);
		}

		return createMediaSources(srcRefs);
	}

	@Override
	public Collection<MediaSource> getMediaSrcs(MediaType mediaType) {
		Client client = clientPool.acquireSync();

		List<KmsMediaObjectRef> srcRefs;

		try {
			srcRefs = client.getMediaSrcsByMediaType(
					this.objectRef.getThriftRef(), mediaType.asKmsType());
		} catch (KmsMediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			this.clientPool.release(client);
		}

		return createMediaSources(srcRefs);

	}

	@Override
	public Collection<MediaSource> getMediaSrcs(MediaType mediaType,
			String description) {

		Client client = clientPool.acquireSync();

		List<KmsMediaObjectRef> srcRefs;

		try {
			srcRefs = client.getMediaSrcsByFullDescription(
					this.objectRef.getThriftRef(), mediaType.asKmsType(),
					description);
		} catch (KmsMediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			this.clientPool.release(client);
		}

		return createMediaSources(srcRefs);

	}

	@Override
	public Collection<MediaSink> getMediaSinks() {

		Client client = clientPool.acquireSync();

		List<KmsMediaObjectRef> sinkRefs;

		try {
			sinkRefs = client.getMediaSinks(this.objectRef.getThriftRef());
		} catch (KmsMediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			this.clientPool.release(client);
		}

		return createMediaSinks(sinkRefs);

	}

	@Override
	public Collection<MediaSink> getMediaSinks(MediaType mediaType) {
		Client client = clientPool.acquireSync();

		List<KmsMediaObjectRef> sinkRefs;

		try {
			sinkRefs = client.getMediaSinksByMediaType(
					this.objectRef.getThriftRef(), mediaType.asKmsType());
		} catch (KmsMediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			this.clientPool.release(client);
		}

		return createMediaSinks(sinkRefs);

	}

	@Override
	public Collection<MediaSink> getMediaSinks(MediaType mediaType,
			String description) {

		Client client = clientPool.acquireSync();

		List<KmsMediaObjectRef> sinkRefs;

		try {
			sinkRefs = client.getMediaSinksByFullDescription(
					this.objectRef.getThriftRef(), mediaType.asKmsType(),
					description);
		} catch (KmsMediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			this.clientPool.release(client);
		}

		return createMediaSinks(sinkRefs);

	}

	@Override
	public void connect(MediaElement sink, MediaType mediaType) {

		Client client = clientPool.acquireSync();

		KmsMediaObjectRef srcRef = this.objectRef.getThriftRef();
		KmsMediaObjectRef sinkRef = ((AbstractMediaObject) sink).getObjectRef()
				.getThriftRef();
		try {
			client.connectElementsByMediaType(srcRef, sinkRef,
					mediaType.asKmsType());
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
	public void connect(MediaElement sink) {

		Client client = clientPool.acquireSync();

		KmsMediaObjectRef srcRef = this.objectRef.getThriftRef();
		KmsMediaObjectRef sinkRef = ((AbstractMediaObject) sink).getObjectRef()
				.getThriftRef();
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
	public void connect(MediaElement sink, MediaType mediaType,
			String mediaDescription) {

		Client client = clientPool.acquireSync();

		KmsMediaObjectRef srcRef = this.objectRef.getThriftRef();
		KmsMediaObjectRef sinkRef = ((AbstractMediaObject) sink).getObjectRef()
				.getThriftRef();
		try {
			client.connectElementsByFullDescription(srcRef, sinkRef,
					mediaType.asKmsType(), mediaDescription);
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
	public void getMediaSrcs(final Continuation<Collection<MediaSource>> cont) {
		final AsyncClient client = clientPool.acquireAsync();
		try {
			client.getMediaSrcs(this.objectRef.getThriftRef(),
					new AsyncMethodCallback<getMediaSrcs_call>() {

						@Override
						public void onComplete(getMediaSrcs_call response) {
							List<KmsMediaObjectRef> srcRefs;
							try {
								srcRefs = response.getResult();
							} catch (KmsMediaServerException e) {
								throw new KurentoMediaFrameworkException(e
										.getMessage(), e, e.getErrorCode());
							} catch (TException e) {
								// TODO change error code
								throw new KurentoMediaFrameworkException(e
										.getMessage(), e, 30000);
							} finally {
								MediaElementImpl.this.clientPool
										.release(client);
							}
							cont.onSuccess(createMediaSources(srcRefs));
						}

						@Override
						public void onError(Exception exception) {
							MediaElementImpl.this.clientPool.release(client);
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
	public void getMediaSrcs(MediaType mediaType,
			final Continuation<Collection<MediaSource>> cont) {
		final AsyncClient client = clientPool.acquireAsync();

		try {
			client.getMediaSrcsByMediaType(this.objectRef.getThriftRef(),
					mediaType.asKmsType(),
					new AsyncMethodCallback<getMediaSrcsByMediaType_call>() {

						@Override
						public void onComplete(
								getMediaSrcsByMediaType_call response) {
							List<KmsMediaObjectRef> srcRefs;
							try {
								srcRefs = response.getResult();
							} catch (KmsMediaServerException e) {
								throw new KurentoMediaFrameworkException(e
										.getMessage(), e, e.getErrorCode());
							} catch (TException e) {
								// TODO change error code
								throw new KurentoMediaFrameworkException(e
										.getMessage(), e, 30000);
							} finally {
								MediaElementImpl.this.clientPool
										.release(client);
							}
							cont.onSuccess(createMediaSources(srcRefs));
						}

						@Override
						public void onError(Exception exception) {
							MediaElementImpl.this.clientPool.release(client);
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
	public void getMediaSrcs(MediaType mediaType, String description,
			final Continuation<Collection<MediaSource>> cont) {
		final AsyncClient client = clientPool.acquireAsync();
		try {
			client.getMediaSrcsByFullDescription(
					this.objectRef.getThriftRef(),
					mediaType.asKmsType(),
					description,
					new AsyncMethodCallback<getMediaSrcsByFullDescription_call>() {

						@Override
						public void onComplete(
								getMediaSrcsByFullDescription_call response) {
							List<KmsMediaObjectRef> srcRefs;
							try {
								srcRefs = response.getResult();
							} catch (KmsMediaServerException e) {
								throw new KurentoMediaFrameworkException(e
										.getMessage(), e, e.getErrorCode());
							} catch (TException e) {
								// TODO change error code
								throw new KurentoMediaFrameworkException(e
										.getMessage(), e, 30000);
							} finally {
								MediaElementImpl.this.clientPool
										.release(client);
							}
							cont.onSuccess(createMediaSources(srcRefs));
						}

						@Override
						public void onError(Exception exception) {
							MediaElementImpl.this.clientPool.release(client);
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
	public void getMediaSinks(final Continuation<Collection<MediaSink>> cont) {
		final AsyncClient client = clientPool.acquireAsync();
		try {
			client.getMediaSinks(this.objectRef.getThriftRef(),
					new AsyncMethodCallback<AsyncClient.getMediaSinks_call>() {

						@Override
						public void onComplete(
								AsyncClient.getMediaSinks_call response) {
							List<KmsMediaObjectRef> sinkRefs;
							try {
								sinkRefs = response.getResult();
							} catch (KmsMediaServerException e) {
								throw new KurentoMediaFrameworkException(e
										.getMessage(), e, e.getErrorCode());
							} catch (TException e) {
								// TODO change error code
								throw new KurentoMediaFrameworkException(e
										.getMessage(), e, 30000);
							} finally {
								MediaElementImpl.this.clientPool
										.release(client);
							}
							cont.onSuccess(createMediaSinks(sinkRefs));
						}

						@Override
						public void onError(Exception exception) {
							MediaElementImpl.this.clientPool.release(client);
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
	public void getMediaSinks(MediaType mediaType,
			final Continuation<Collection<MediaSink>> cont) {
		final AsyncClient client = clientPool.acquireAsync();
		try {
			client.getMediaSinksByMediaType(this.objectRef.getThriftRef(),
					mediaType.asKmsType(),
					new AsyncMethodCallback<getMediaSinksByMediaType_call>() {

						@Override
						public void onComplete(
								getMediaSinksByMediaType_call response) {
							List<KmsMediaObjectRef> sinkRefs;
							try {
								sinkRefs = response.getResult();
							} catch (KmsMediaServerException e) {
								throw new KurentoMediaFrameworkException(e
										.getMessage(), e, e.getErrorCode());
							} catch (TException e) {
								// TODO change error code
								throw new KurentoMediaFrameworkException(e
										.getMessage(), e, 30000);
							} finally {
								MediaElementImpl.this.clientPool
										.release(client);
							}
							cont.onSuccess(createMediaSinks(sinkRefs));
						}

						@Override
						public void onError(Exception exception) {
							MediaElementImpl.this.clientPool.release(client);
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
	public void getMediaSinks(MediaType mediaType, String description,
			final Continuation<Collection<MediaSink>> cont) {
		final AsyncClient client = clientPool.acquireAsync();
		try {
			client.getMediaSinksByFullDescription(
					this.objectRef.getThriftRef(),
					mediaType.asKmsType(),
					description,
					new AsyncMethodCallback<getMediaSinksByFullDescription_call>() {

						@Override
						public void onComplete(
								getMediaSinksByFullDescription_call response) {
							List<KmsMediaObjectRef> sinkRefs;
							try {
								sinkRefs = response.getResult();
							} catch (KmsMediaServerException e) {
								throw new KurentoMediaFrameworkException(e
										.getMessage(), e, e.getErrorCode());
							} catch (TException e) {
								// TODO change error code
								throw new KurentoMediaFrameworkException(e
										.getMessage(), e, 30000);
							} finally {
								MediaElementImpl.this.clientPool
										.release(client);
							}

							cont.onSuccess(createMediaSinks(sinkRefs));
						}

						@Override
						public void onError(Exception exception) {
							MediaElementImpl.this.clientPool.release(client);
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
	public void connect(final MediaElement sink, final MediaType mediaType,
			final Continuation<Void> cont) {
		final AsyncClient client = clientPool.acquireAsync();

		final KmsMediaObjectRef srcRef = this.objectRef.getThriftRef();
		final KmsMediaObjectRef sinkRef = ((AbstractMediaObject) sink)
				.getObjectRef().getThriftRef();

		try {
			client.connectElementsByMediaType(srcRef, sinkRef,
					mediaType.asKmsType(),
					new AsyncMethodCallback<connectElementsByMediaType_call>() {

						@Override
						public void onComplete(
								connectElementsByMediaType_call response) {
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
								MediaElementImpl.this.clientPool
										.release(client);
							}
							log.debug(
									"Object {}: Async. connection to sink {} succeeded}",
									getId(), Long.valueOf(sinkRef.getId()));
							cont.onSuccess(null);
						}

						@Override
						public void onError(Exception exception) {
							MediaElementImpl.this.clientPool.release(client);
							log.error(
									"Object {}: Async. connection to sink {} failed}",
									getId(), Long.valueOf(sinkRef.getId()));
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
	public void connect(MediaElement sink, final Continuation<Void> cont) {
		final AsyncClient client = clientPool.acquireAsync();

		final KmsMediaObjectRef srcRef = this.objectRef.getThriftRef();
		final KmsMediaObjectRef sinkRef = ((AbstractMediaObject) sink)
				.getObjectRef().getThriftRef();

		try {
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
								MediaElementImpl.this.clientPool
										.release(client);
							}
							log.debug(
									"Object {}: Async. connection to sink {} succeeded}",
									getId(), Long.valueOf(sinkRef.getId()));
							cont.onSuccess(null);
						}

						@Override
						public void onError(Exception exception) {
							MediaElementImpl.this.clientPool.release(client);
							log.error(
									"Object {}: Async. connection to sink {} failed}",
									getId(), Long.valueOf(sinkRef.getId()));
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
	public void connect(final MediaElement sink, final MediaType mediaType,
			final String mediaDescription, final Continuation<Void> cont) {
		final AsyncClient client = clientPool.acquireAsync();

		final KmsMediaObjectRef srcRef = this.objectRef.getThriftRef();
		final KmsMediaObjectRef sinkRef = ((AbstractMediaObject) sink)
				.getObjectRef().getThriftRef();

		try {
			client.connectElementsByFullDescription(
					srcRef,
					sinkRef,
					mediaType.asKmsType(),
					mediaDescription,
					new AsyncMethodCallback<connectElementsByFullDescription_call>() {

						@Override
						public void onComplete(
								connectElementsByFullDescription_call response) {
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
								MediaElementImpl.this.clientPool
										.release(client);
							}
							log.debug(
									"Object {}: Async. connection to sink {} succeeded}",
									getId(), Long.valueOf(sinkRef.getId()));
							cont.onSuccess(null);
						}

						@Override
						public void onError(Exception exception) {
							MediaElementImpl.this.clientPool.release(client);
							log.error(
									"Object {}: Async. connection to sink {} failed}",
									getId(), Long.valueOf(sinkRef.getId()));
							cont.onError(exception);
						}
					});

		} catch (TException e) {
			this.clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

	private Collection<MediaSink> createMediaSinks(
			Collection<KmsMediaObjectRef> sinkRefs) {
		Collection<MediaSink> sinks = new ArrayList<MediaSink>(sinkRefs.size());
		for (KmsMediaObjectRef padRef : sinkRefs) {
			MediaSinkImpl sink = (MediaSinkImpl) this.ctx.getBean(
					"mediaObject", new MediaPadRef(padRef));
			sinks.add(sink);
		}

		return sinks;

	}

	private Collection<MediaSource> createMediaSources(
			List<KmsMediaObjectRef> srcRefs) {
		Collection<MediaSource> srcs = new ArrayList<MediaSource>(
				srcRefs.size());
		for (KmsMediaObjectRef padRef : srcRefs) {
			MediaSourceImpl src = (MediaSourceImpl) this.ctx.getBean(
					"mediaObject", new MediaPadRef(padRef));
			srcs.add(src);
		}

		return srcs;

	}

	static class MediaElementBuilderImpl<T extends MediaElementBuilderImpl<T, E>, E extends MediaElement>
			extends AbstractCollectableMediaObjectBuilder<T, E> {

		/**
		 * @param elementType
		 */
		protected MediaElementBuilderImpl(final String elementType,
				final MediaPipeline pipeline) {
			super(elementType, pipeline, 0);
		}

		@SuppressWarnings("unchecked")
		@Override
		public E build() {
			return (E) pipeline.createMediaElement(elementName, params);
		}

		@Override
		public void buildAsync(Continuation<E> cont) {
			pipeline.createMediaElement(elementName, params, cont);
		}

	}

}
