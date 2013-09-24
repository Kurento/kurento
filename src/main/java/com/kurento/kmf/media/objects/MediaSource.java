package com.kurento.kmf.media.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.IsMediaElement;
import com.kurento.kmf.media.internal.refs.MediaPadRefDTO;
import com.kurento.kms.thrift.api.MediaObjectRef;
import com.kurento.kms.thrift.api.MediaServerException;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.MediaServerService.Client;

@IsMediaElement(type = MediaSource.TYPE)
public class MediaSource extends MediaPad {

	public static final String TYPE = "MediaSource";

	// TODO this is public cause it´s used in MediaApiAppContextConfiguration
	public MediaSource(MediaPadRefDTO objectRef) {
		super(objectRef);
	}

	/**
	 * Connects the current source with a {@link MediaSink}
	 * 
	 * @param sink
	 *            The sink to connect this source
	 * @throws MediaServerException
	 * @throws InvokationException
	 */
	public void connect(MediaSink sink) throws KurentoMediaFrameworkException {

		final Client client = clientPool.acquireSync();

		try {
			client.connect(objectRef.getThriftRef(),
					sink.objectRef.getThriftRef());
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}
	}

	/**
	 * Gets all {@link MediaSink} to which this source is connected.
	 * 
	 * @return The list of sinks that the source is connected to.
	 * @throws MediaServerException
	 * @throws InvokationException
	 */
	public Collection<MediaSink> getConnectedSinks()
			throws KurentoMediaFrameworkException {
		final Client client = clientPool.acquireSync();

		List<MediaObjectRef> sinkRefs;

		try {
			sinkRefs = client.getConnectedSinks(objectRef.getThriftRef());
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
	 * Connects the current source with a {@link MediaSink}
	 * 
	 * @param sink
	 *            The sink to connect this source
	 * @throws MediaServerException
	 * @throws InvokationException
	 */
	public void connect(MediaSink sink, final Continuation<Void> cont)
			throws KurentoMediaFrameworkException {
		final AsyncClient client = clientPool.acquireAsync();

		try {
			client.connect(objectRef.getThriftRef(),
					sink.objectRef.getThriftRef(),
					new AsyncMethodCallback<AsyncClient.connect_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
						}

						@Override
						public void onComplete(AsyncClient.connect_call response) {
							try {
								response.getResult();
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
							cont.onSuccess(null);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
	}

	/**
	 * Gets all {@link MediaSink} to which this source is connected.
	 * 
	 * @return The list of sinks that the source is connected to.
	 * @throws MediaServerException
	 * @throws InvokationException
	 */
	public void getConnectedSinks(final Continuation<Collection<MediaSink>> cont)
			throws KurentoMediaFrameworkException {
		final AsyncClient client = clientPool.acquireAsync();

		try {
			client.getConnectedSinks(
					objectRef.getThriftRef(),
					new AsyncMethodCallback<AsyncClient.getConnectedSinks_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@Override
						public void onComplete(
								AsyncClient.getConnectedSinks_call response) {
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

}
