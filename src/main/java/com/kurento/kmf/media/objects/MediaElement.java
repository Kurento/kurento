package com.kurento.kmf.media.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.Continuation;
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

public abstract class MediaElement extends MediaObject {

	public MediaElement(MediaElementRefDTO objectRef) {
		super(objectRef);
	}

	/**
	 * Returns all {@link MediaSource} from this element
	 * 
	 * @return A list of sources. The list will be empty if no sources are
	 *         found.
	 * @throws MediaServerException
	 * @throws InvokationException
	 */
	public Collection<MediaSource> getMediaSrcs()
			throws KurentoMediaFrameworkException {

		Client client = clientPool.acquireSync();

		List<MediaObjectRef> srcRefs;

		try {
			srcRefs = client.getMediaSrcs(objectRef.getThriftRef());
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}

		return createMediaSources(srcRefs);
	}

	/**
	 * Returns {@link MediaSource} of a certain type, associated to this element
	 * 
	 * @param mediaType
	 *            The type of media from the sources
	 * @return A list of sources. The list will be empty if no sources are
	 *         found.
	 * @throws MediaServerException
	 * @throws InvokationException
	 */
	public Collection<MediaSource> getMediaSrcs(MediaType mediaType)
			throws KurentoMediaFrameworkException {
		Client client = clientPool.acquireSync();

		List<MediaObjectRef> srcRefs;

		try {
			srcRefs = client.getMediaSrcsByMediaType(objectRef.getThriftRef(),
					mediaType);
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}

		return createMediaSources(srcRefs);
	}

	/**
	 * Returns {@link MediaSource} of a certain type and description, associated
	 * to this element
	 * 
	 * @param mediaType
	 *            The type of media from the sources
	 * @param Description
	 * @return A list of sources. The list will be empty if no sources are
	 *         found.
	 * @throws MediaServerException
	 * @throws InvokationException
	 */
	public Collection<MediaSource> getMediaSrcs(MediaType mediaType,
			String description) throws KurentoMediaFrameworkException {

		Client client = clientPool.acquireSync();

		List<MediaObjectRef> srcRefs;

		try {
			srcRefs = client.getMediaSrcsByFullDescription(
					objectRef.getThriftRef(), mediaType, description);
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}

		return createMediaSources(srcRefs);
	}

	/**
	 * 
	 * @return A list of sinks. The list will be empty if no sinks are found.
	 * @throws MediaServerException
	 * @throws InvokationException
	 */
	public Collection<MediaSink> getMediaSinks()
			throws KurentoMediaFrameworkException {

		Client client = clientPool.acquireSync();

		List<MediaObjectRef> sinkRefs;

		try {
			sinkRefs = client.getMediaSinks(objectRef.getThriftRef());
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}

		return createMediaSinks(sinkRefs);
	}

	/**
	 * Returns {@link MediaSink} of a certain type, associated to this element
	 * 
	 * @param mediaType
	 *            The type of media from the sink
	 * @return A list of sinks. The list will be empty if no sinks are found.
	 * @throws MediaServerException
	 * @throws InvokationException
	 */
	public Collection<MediaSink> getMediaSinks(MediaType mediaType)
			throws KurentoMediaFrameworkException {
		Client client = clientPool.acquireSync();

		List<MediaObjectRef> sinkRefs;

		try {
			sinkRefs = client.getMediaSinksByMediaType(
					objectRef.getThriftRef(), mediaType);
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}

		return createMediaSinks(sinkRefs);
	}

	/**
	 * Returns {@link MediaSink} of a certain type and description, associated
	 * to this element
	 * 
	 * @param mediaType
	 *            The type of media from the sink
	 * @param description
	 * @return A list of sinks. The list will be empty if no sinks are found.
	 * @throws MediaServerException
	 * @throws InvokationException
	 */
	public Collection<MediaSink> getMediaSinks(MediaType mediaType,
			String description) throws KurentoMediaFrameworkException {

		Client client = clientPool.acquireSync();

		List<MediaObjectRef> sinkRefs;

		try {
			sinkRefs = client.getMediaSinksByFullDescription(
					objectRef.getThriftRef(), mediaType, description);
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}

		return createMediaSinks(sinkRefs);
	}

	/**
	 * Returns all {@link MediaSource} from this element
	 * 
	 * @return A list of sources. The list will be empty if no sources are
	 *         found.
	 * @throws MediaServerException
	 * @throws InvokationException
	 */
	public void getMediaSrcs(final Continuation<Collection<MediaSource>> cont)
			throws KurentoMediaFrameworkException {
		final AsyncClient client = clientPool.acquireAsync();
		try {
			client.getMediaSrcs(objectRef.getThriftRef(),
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
								clientPool.release(client);
							}
							cont.onSuccess(createMediaSources(srcRefs));
						}

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

	/**
	 * Returns {@link MediaSource} of a certain type, associated to this element
	 * 
	 * @param mediaType
	 *            The type of media from the sources
	 * @return A list of sources. The list will be empty if no sources are
	 *         found.
	 * @throws MediaServerException
	 * @throws InvokationException
	 */
	public void getMediaSrcs(MediaType mediaType,
			final Continuation<Collection<MediaSource>> cont)
			throws KurentoMediaFrameworkException {
		final AsyncClient client = clientPool.acquireAsync();

		try {
			client.getMediaSrcsByMediaType(objectRef.getThriftRef(), mediaType,
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
								clientPool.release(client);
							}
							cont.onSuccess(createMediaSources(srcRefs));
						}

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

	/**
	 * Returns {@link MediaSource} of a certain type and description, associated
	 * to this element
	 * 
	 * @param mediaType
	 *            The type of media from the sources
	 * @param Description
	 * @return A list of sources. The list will be empty if no sources are
	 *         found.
	 * @throws MediaServerException
	 * @throws InvokationException
	 */
	public void getMediaSrcs(MediaType mediaType, String description,
			final Continuation<Collection<MediaSource>> cont)
			throws KurentoMediaFrameworkException {
		final AsyncClient client = clientPool.acquireAsync();
		try {
			client.getMediaSrcsByFullDescription(
					objectRef.getThriftRef(),
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
								clientPool.release(client);
							}
							cont.onSuccess(createMediaSources(srcRefs));
						}

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

	/**
	 * 
	 * @return A list of sinks. The list will be empty if no sinks are found.
	 * @throws MediaServerException
	 * @throws InvokationException
	 */
	public void getMediaSinks(final Continuation<Collection<MediaSink>> cont)
			throws KurentoMediaFrameworkException {
		final AsyncClient client = clientPool.acquireAsync();
		try {
			client.getMediaSinks(objectRef.getThriftRef(),
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
								clientPool.release(client);
							}
							cont.onSuccess(createMediaSinks(sinkRefs));
						}

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

	/**
	 * Returns {@link MediaSink} of a certain type, associated to this element
	 * 
	 * @param mediaType
	 *            The type of media from the sink
	 * @return A list of sinks. The list will be empty if no sinks are found.
	 * @throws MediaServerException
	 * @throws InvokationException
	 */
	public void getMediaSinks(MediaType mediaType,
			final Continuation<Collection<MediaSink>> cont)
			throws KurentoMediaFrameworkException {
		final AsyncClient client = clientPool.acquireAsync();
		try {
			client.getMediaSinksByMediaType(objectRef.getThriftRef(),
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
								clientPool.release(client);
							}
							cont.onSuccess(createMediaSinks(sinkRefs));
						}

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

	/**
	 * Returns {@link MediaSink} of a certain type and description, associated
	 * to this element
	 * 
	 * @param mediaType
	 *            The type of media from the sink
	 * @param description
	 * @return A list of sinks. The list will be empty if no sinks are found.
	 * @throws MediaServerException
	 * @throws InvokationException
	 */
	public void getMediaSinks(MediaType mediaType, String description,
			final Continuation<Collection<MediaSink>> cont)
			throws KurentoMediaFrameworkException {
		final AsyncClient client = clientPool.acquireAsync();
		try {
			client.getMediaSinksByFullDescription(
					objectRef.getThriftRef(),
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
								clientPool.release(client);
							}

							cont.onSuccess(createMediaSinks(sinkRefs));
						}

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

	private Collection<MediaSink> createMediaSinks(
			Collection<MediaObjectRef> sinkRefs) {
		Collection<MediaSink> sinks = new ArrayList<MediaSink>(sinkRefs.size());
		for (MediaObjectRef padRef : sinkRefs) {
			MediaSink sink = (MediaSink) ctx.getBean("mediaObject",
					new MediaPadRefDTO(padRef));
			sinks.add(sink);
		}

		return sinks;
	}

	private Collection<MediaSource> createMediaSources(
			List<MediaObjectRef> srcRefs) {
		Collection<MediaSource> srcs = new ArrayList<MediaSource>(
				srcRefs.size());
		for (MediaObjectRef padRef : srcRefs) {
			MediaSource src = (MediaSource) ctx.getBean("mediaObject",
					new MediaPadRefDTO(padRef));
			srcs.add(src);
		}

		return srcs;
	}

}
