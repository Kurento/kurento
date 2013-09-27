package com.kurento.kmf.media.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaSink;
import com.kurento.kmf.media.MediaSource;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;
import com.kurento.kmf.media.internal.refs.MediaPadRefDTO;
import com.kurento.kms.thrift.api.MediaObjectRef;
import com.kurento.kms.thrift.api.MediaServerException;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.getMediaSinksByFullDescription_call;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.getMediaSinksByMediaType_call;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.getMediaSrcsByFullDescription_call;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.getMediaSrcsByMediaType_call;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.getMediaSrcs_call;
import com.kurento.kms.thrift.api.MediaServerService.Client;
import com.kurento.kms.thrift.api.MediaType;

public class MediaElementImpl extends AbstractMediaObject implements
		MediaElement {

	public MediaElementImpl(MediaElementRefDTO objectRef) {
		super(objectRef);
	}

	@Override
	public Collection<MediaSource> getMediaSrcs() {

		Client client = this.clientPool.acquireSync();

		List<MediaObjectRef> srcRefs;

		try {
			srcRefs = client.getMediaSrcs(this.objectRef.getThriftRef());
		} catch (MediaServerException e) {
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
		Client client = this.clientPool.acquireSync();

		List<MediaObjectRef> srcRefs;

		try {
			srcRefs = client.getMediaSrcsByMediaType(
					this.objectRef.getThriftRef(), mediaType);
		} catch (MediaServerException e) {
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

		Client client = this.clientPool.acquireSync();

		List<MediaObjectRef> srcRefs;

		try {
			srcRefs = client.getMediaSrcsByFullDescription(
					this.objectRef.getThriftRef(), mediaType, description);
		} catch (MediaServerException e) {
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

		Client client = this.clientPool.acquireSync();

		List<MediaObjectRef> sinkRefs;

		try {
			sinkRefs = client.getMediaSinks(this.objectRef.getThriftRef());
		} catch (MediaServerException e) {
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
		Client client = this.clientPool.acquireSync();

		List<MediaObjectRef> sinkRefs;

		try {
			sinkRefs = client.getMediaSinksByMediaType(
					this.objectRef.getThriftRef(), mediaType);
		} catch (MediaServerException e) {
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

		Client client = this.clientPool.acquireSync();

		List<MediaObjectRef> sinkRefs;

		try {
			sinkRefs = client.getMediaSinksByFullDescription(
					this.objectRef.getThriftRef(), mediaType, description);
		} catch (MediaServerException e) {
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

		Collection<MediaSource> sources = this.getMediaSrcs(mediaType);
		Collection<MediaSink> sinks = sink.getMediaSinks(mediaType);

		if (sources.size() != sinks.size()) {
			throw new KurentoMediaFrameworkException("Cannot connect "
					+ sources.size() + " sources to " + sinks.size()
					+ " sinks. Perform connect individually", 30000); // TODO
		}

		// If there is nothing to connect, return
		if (sinks.size() == 0) {
			return;
		}

		// Map all sinks to their description (null description supported)
		HashMap<String, MediaSink> descriptionToSinkMap = new HashMap<String, MediaSink>();
		for (MediaSink snk : sinks) {
			descriptionToSinkMap.put(snk.getMediaDescription(), snk);
		}

		if (descriptionToSinkMap.size() != sinks.size()) {
			throw new KurentoMediaFrameworkException(
					"Cannot connect to sinks having duplicate media descriptions",
					30000); // TODO
		}

		HashMap<String, MediaSource> descriptionToSourceMap = new HashMap<String, MediaSource>();
		for (MediaSource src : sources) {
			descriptionToSourceMap.put(src.getMediaDescription(), src);
		}

		if (descriptionToSourceMap.size() != sources.size()) {
			throw new KurentoMediaFrameworkException(
					"Cannot connect from sources having duplicate media descriptions",
					30000); // TODO
		}

		if (!descriptionToSinkMap.keySet().equals(
				descriptionToSourceMap.keySet())) {
			throw new KurentoMediaFrameworkException(
					"Cannot connect sources to sinks with different media descriptioins",
					30000); // TODO
		}

		for (String mediaDescription : descriptionToSourceMap.keySet()) {
			descriptionToSourceMap.get(mediaDescription).connect(
					descriptionToSinkMap.get(mediaDescription));
		}
	}

	@Override
	public void connect(MediaElement sink) {
		connect(sink, MediaType.VIDEO);
		connect(sink, MediaType.AUDIO);
		connect(sink, MediaType.DATA);
	}

	@Override
	public void connect(MediaElement sink, MediaType mediaType,
			String mediaDescription) {
		Collection<MediaSource> sources = this.getMediaSrcs(mediaType,
				mediaDescription);
		if (sources.size() > 1) {
			throw new KurentoMediaFrameworkException(
					"Cannot connect having multiple sources with the same media description",
					30000); // TODO
		}

		Collection<MediaSink> sinks = sink.getMediaSinks(mediaType,
				mediaDescription);

		if (sources.size() != sinks.size()) {
			throw new KurentoMediaFrameworkException(
					"Cannot connect to sinks with different cardinality", 30000); // TODO
		}

		if (sources.size() == 0) {
			return;
		}

		sources.iterator().next().connect(sinks.iterator().next());

	}

	@Override
	public void getMediaSrcs(final Continuation<Collection<MediaSource>> cont) {
		final AsyncClient client = this.clientPool.acquireAsync();
		try {
			client.getMediaSrcs(this.objectRef.getThriftRef(),
					new AsyncMethodCallback<getMediaSrcs_call>() {

						@Override
						public void onComplete(getMediaSrcs_call response) {
							List<MediaObjectRef> srcRefs;
							try {
								srcRefs = response.getResult();
							} catch (MediaServerException e) {
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
		final AsyncClient client = this.clientPool.acquireAsync();

		try {
			client.getMediaSrcsByMediaType(this.objectRef.getThriftRef(),
					mediaType,
					new AsyncMethodCallback<getMediaSrcsByMediaType_call>() {

						@Override
						public void onComplete(
								getMediaSrcsByMediaType_call response) {
							List<MediaObjectRef> srcRefs;
							try {
								srcRefs = response.getResult();
							} catch (MediaServerException e) {
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
		final AsyncClient client = this.clientPool.acquireAsync();
		try {
			client.getMediaSrcsByFullDescription(
					this.objectRef.getThriftRef(),
					mediaType,
					description,
					new AsyncMethodCallback<getMediaSrcsByFullDescription_call>() {

						@Override
						public void onComplete(
								getMediaSrcsByFullDescription_call response) {
							List<MediaObjectRef> srcRefs;
							try {
								srcRefs = response.getResult();
							} catch (MediaServerException e) {
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
		final AsyncClient client = this.clientPool.acquireAsync();
		try {
			client.getMediaSinks(this.objectRef.getThriftRef(),
					new AsyncMethodCallback<AsyncClient.getMediaSinks_call>() {

						@Override
						public void onComplete(
								AsyncClient.getMediaSinks_call response) {
							List<MediaObjectRef> sinkRefs;
							try {
								sinkRefs = response.getResult();
							} catch (MediaServerException e) {
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
		final AsyncClient client = this.clientPool.acquireAsync();
		try {
			client.getMediaSinksByMediaType(this.objectRef.getThriftRef(),
					mediaType,
					new AsyncMethodCallback<getMediaSinksByMediaType_call>() {

						@Override
						public void onComplete(
								getMediaSinksByMediaType_call response) {
							List<MediaObjectRef> sinkRefs;
							try {
								sinkRefs = response.getResult();
							} catch (MediaServerException e) {
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
		final AsyncClient client = this.clientPool.acquireAsync();
		try {
			client.getMediaSinksByFullDescription(
					this.objectRef.getThriftRef(),
					mediaType,
					description,
					new AsyncMethodCallback<getMediaSinksByFullDescription_call>() {

						@Override
						public void onComplete(
								getMediaSinksByFullDescription_call response) {
							List<MediaObjectRef> sinkRefs;
							try {
								sinkRefs = response.getResult();
							} catch (MediaServerException e) {
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
			Collection<MediaObjectRef> sinkRefs) {
		Collection<MediaSink> sinks = new ArrayList<MediaSink>(sinkRefs.size());
		for (MediaObjectRef padRef : sinkRefs) {
			MediaSinkImpl sink = (MediaSinkImpl) this.ctx.getBean(
					"mediaObject", new MediaPadRefDTO(padRef));
			sinks.add(sink);
		}

		return sinks;
	}

	private Collection<MediaSource> createMediaSources(
			List<MediaObjectRef> srcRefs) {
		Collection<MediaSource> srcs = new ArrayList<MediaSource>(
				srcRefs.size());
		for (MediaObjectRef padRef : srcRefs) {
			MediaSourceImpl src = (MediaSourceImpl) this.ctx.getBean(
					"mediaObject", new MediaPadRefDTO(padRef));
			srcs.add(src);
		}

		return srcs;
	}

}
