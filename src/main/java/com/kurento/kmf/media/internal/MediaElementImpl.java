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

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaSink;
import com.kurento.kmf.media.MediaSource;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.internal.refs.MediaPadRef;
import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kms.thrift.api.KmsMediaObjectRef;
import com.kurento.kms.thrift.api.KmsMediaServerException;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.getMediaSinksByFullDescription_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.getMediaSinksByMediaType_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.getMediaSrcsByFullDescription_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.getMediaSrcsByMediaType_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.getMediaSrcs_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.Client;
import com.kurento.kms.thrift.api.KmsMediaType;

public class MediaElementImpl extends AbstractCollectableMediaObject implements
		MediaElement {

	public MediaElementImpl(MediaElementRef objectRef) {
		super(objectRef);
	}

	public MediaElementImpl(MediaElementRef objectRef,
			Map<String, MediaParam> params) {
		super(objectRef, params);
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
	public Collection<MediaSource> getMediaSrcs(KmsMediaType mediaType) {
		Client client = clientPool.acquireSync();

		List<KmsMediaObjectRef> srcRefs;

		try {
			srcRefs = client.getMediaSrcsByMediaType(
					this.objectRef.getThriftRef(), mediaType);
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
	public Collection<MediaSource> getMediaSrcs(KmsMediaType mediaType,
			String description) {

		Client client = clientPool.acquireSync();

		List<KmsMediaObjectRef> srcRefs;

		try {
			srcRefs = client.getMediaSrcsByFullDescription(
					this.objectRef.getThriftRef(), mediaType, description);
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
	public Collection<MediaSink> getMediaSinks(KmsMediaType mediaType) {
		Client client = clientPool.acquireSync();

		List<KmsMediaObjectRef> sinkRefs;

		try {
			sinkRefs = client.getMediaSinksByMediaType(
					this.objectRef.getThriftRef(), mediaType);
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
	public Collection<MediaSink> getMediaSinks(KmsMediaType mediaType,
			String description) {

		Client client = clientPool.acquireSync();

		List<KmsMediaObjectRef> sinkRefs;

		try {
			sinkRefs = client.getMediaSinksByFullDescription(
					this.objectRef.getThriftRef(), mediaType, description);
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
	public void connect(MediaElement sink, KmsMediaType mediaType) {
		// TODO this connect should be done in the server
		// Collection<MediaSource> sources = this.getMediaSrcs(mediaType);
		// Collection<MediaSink> sinks = sink.getMediaSinks(mediaType);
		//
		// if (sources.size() != sinks.size()) {
		// // TODO change error code
		// throw new KurentoMediaFrameworkException("Cannot connect "
		// + sources.size() + " sources to " + sinks.size()
		// + " sinks. Perform connect individually", 30000);
		// }
		//
		// // If there is nothing to connect, return
		// if (sinks.size() == 0) {
		// return;
		// }
		//
		// // Map all sinks to their description (null description supported)
		// HashMap<String, MediaSink> descriptionToSinkMap = new HashMap<String,
		// MediaSink>();
		// for (MediaSink snk : sinks) {
		// descriptionToSinkMap.put(snk.getMediaDescription(), snk);
		// }
		//
		// if (descriptionToSinkMap.size() != sinks.size()) {
		// throw new KurentoMediaFrameworkException(
		// "Cannot connect to sinks having duplicate media descriptions",
		// 30000); // TODO change error code
		// }
		//
		// HashMap<String, MediaSource> descriptionToSourceMap = new
		// HashMap<String, MediaSource>();
		// for (MediaSource src : sources) {
		// descriptionToSourceMap.put(src.getMediaDescription(), src);
		// }
		//
		// if (descriptionToSourceMap.size() != sources.size()) {
		// throw new KurentoMediaFrameworkException(
		// "Cannot connect from sources having duplicate media descriptions",
		// 30000); // TODO change error code
		// }
		//
		// if (!descriptionToSinkMap.keySet().equals(
		// descriptionToSourceMap.keySet())) {
		// throw new KurentoMediaFrameworkException(
		// "Cannot connect sources to sinks with different media descriptioins",
		// 30000); // TODO change error code
		// }
		//
		// for (String mediaDescription : descriptionToSourceMap.keySet()) {
		// descriptionToSourceMap.get(mediaDescription).connect(
		// descriptionToSinkMap.get(mediaDescription));
		// }

	}

	@Override
	public void connect(MediaElement sink) {
		// TODO this connect should be done in the server
		// connect(sink, KmsMediaType.VIDEO);
		// connect(sink, KmsMediaType.AUDIO);
		// connect(sink, KmsMediaType.DATA);

	}

	@Override
	public void connect(MediaElement sink, KmsMediaType mediaType,
			String mediaDescription) {
		// TODO this connect should be done in the server
		// Collection<MediaSource> sources = this.getMediaSrcs(mediaType,
		// mediaDescription);
		// if (sources.size() > 1) {
		// throw new KurentoMediaFrameworkException(
		// "Cannot connect having multiple sources with the same media description",
		// 30000); // TODO change error code
		// }
		//
		// Collection<MediaSink> sinks = sink.getMediaSinks(mediaType,
		// mediaDescription);
		//
		// if (sources.size() != sinks.size()) {
		// // TODO change error code
		// throw new KurentoMediaFrameworkException(
		// "Cannot connect to sinks with different cardinality", 30000);
		// }
		//
		// if (sources.size() == 0) {
		// return;
		// }
		//
		// sources.iterator().next().connect(sinks.iterator().next());

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
	public void getMediaSrcs(KmsMediaType mediaType,
			final Continuation<Collection<MediaSource>> cont) {
		final AsyncClient client = clientPool.acquireAsync();

		try {
			client.getMediaSrcsByMediaType(this.objectRef.getThriftRef(),
					mediaType,
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
	public void getMediaSrcs(KmsMediaType mediaType, String description,
			final Continuation<Collection<MediaSource>> cont) {
		final AsyncClient client = clientPool.acquireAsync();
		try {
			client.getMediaSrcsByFullDescription(
					this.objectRef.getThriftRef(),
					mediaType,
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
	public void getMediaSinks(KmsMediaType mediaType,
			final Continuation<Collection<MediaSink>> cont) {
		final AsyncClient client = clientPool.acquireAsync();
		try {
			client.getMediaSinksByMediaType(this.objectRef.getThriftRef(),
					mediaType,
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
	public void getMediaSinks(KmsMediaType mediaType, String description,
			final Continuation<Collection<MediaSink>> cont) {
		final AsyncClient client = clientPool.acquireAsync();
		try {
			client.getMediaSinksByFullDescription(
					this.objectRef.getThriftRef(),
					mediaType,
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

}
