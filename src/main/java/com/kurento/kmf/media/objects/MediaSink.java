package com.kurento.kmf.media.objects;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.IsMediaElement;
import com.kurento.kmf.media.internal.refs.MediaPadRefDTO;
import com.kurento.kms.thrift.api.MediaServerException;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.MediaServerService.Client;

@IsMediaElement(type = MediaSink.TYPE)
public class MediaSink extends MediaPad {

	public static final String TYPE = "MediaSink";

	// TODO this is public cause it´s used in MediaApiAppContextConfiguration
	public MediaSink(MediaPadRefDTO objectRef) {
		super(objectRef);
	}

	/**
	 * Disconnects the current sink from the referred {@link MediaSource}
	 * 
	 * @param src
	 *            The source to disconnect
	 * @throws MediaServerException
	 * @throws InvokationException
	 */
	public void disconnect(MediaSource src)
			throws KurentoMediaFrameworkException {

		Client client = this.clientPool.acquireSync();

		try {
			client.disconnect(src.objectRef.getThriftRef(),
					this.objectRef.getThriftRef());
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO Change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			this.clientPool.release(client);
		}
	}

	/**
	 * Gets the {@link MediaSource} that is connected to this sink.
	 * 
	 * @return The source connected to this sink.
	 * @throws MediaServerException
	 * @throws InvokationException
	 */
	public MediaSource getConnectedSrc() throws KurentoMediaFrameworkException {

		Client client = this.clientPool.acquireSync();

		MediaPadRefDTO padRefDTO;
		try {
			padRefDTO = new MediaPadRefDTO(
					client.getConnectedSrc(this.objectRef.getThriftRef()));
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} catch (TException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			this.clientPool.release(client);
		}

		MediaSource source = (MediaSource) ctx
				.getBean("mediaObject", padRefDTO);
		return source;
	}

	/**
	 * Connects the current source with a {@link MediaSink}
	 * 
	 * @param sink
	 *            The sink to connect this source
	 * @throws MediaServerException
	 * @throws InvokationException
	 */
	public void disconnect(MediaSink sink, final Continuation<Void> cont)
			throws KurentoMediaFrameworkException {
		final AsyncClient client = this.clientPool.acquireAsync();

		try {
			client.disconnect(this.objectRef.getThriftRef(),
					sink.objectRef.getThriftRef(),
					new AsyncMethodCallback<AsyncClient.disconnect_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@Override
						public void onComplete(
								AsyncClient.disconnect_call response) {
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
	public void getConnectedSrc(final Continuation<MediaSource> cont)
			throws KurentoMediaFrameworkException {
		final AsyncClient client = this.clientPool.acquireAsync();

		try {
			client.getConnectedSrc(
					this.objectRef.getThriftRef(),
					new AsyncMethodCallback<AsyncClient.getConnectedSrc_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@Override
						public void onComplete(
								AsyncClient.getConnectedSrc_call response) {
							MediaPadRefDTO srcRef;
							try {
								srcRef = new MediaPadRefDTO(response
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

							MediaSource src = (MediaSource) ctx.getBean(
									"mediaObject", srcRef);
							cont.onSuccess(src);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}
}
