package com.kurento.kmf.media.objects;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;
import com.kurento.kmf.media.internal.refs.MediaMixerRefDTO;
import com.kurento.kms.thrift.api.Command;
import com.kurento.kms.thrift.api.MediaServerException;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.createMixerEndPointWithParams_call;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.createMixerEndPoint_call;
import com.kurento.kms.thrift.api.MediaServerService.Client;

public abstract class MediaMixer extends MediaObject {

	public MediaMixer(MediaMixerRefDTO objectRef) {
		super(objectRef);
	}

	protected MediaElement createEndPoint()
			throws KurentoMediaFrameworkException {

		Client client = clientPool.acquireSync();

		MediaElementRefDTO endPointRef;
		try {
			endPointRef = new MediaElementRefDTO(
					client.createMixerEndPoint(objectRef.getThriftRef()));
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}

		MediaElement endPoint = (MediaElement) ctx.getBean("mediaObject",
				endPointRef);
		return endPoint;
	}

	protected MediaElement createEndPoint(Command params)
			throws KurentoMediaFrameworkException {
		Client client = clientPool.acquireSync();

		MediaElementRefDTO endPointRef;

		try {
			endPointRef = new MediaElementRefDTO(
					client.createMixerEndPointWithParams(
							this.objectRef.getThriftRef(), params));
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}

		MediaElement endPoint = (MediaElement) ctx.getBean("mediaObject",
				endPointRef);
		return endPoint;
	}

	protected void createEndPoint(final Continuation<MediaElement> cont)
			throws KurentoMediaFrameworkException {
		final AsyncClient client = clientPool.acquireAsync();

		try {
			client.createMixerEndPoint(objectRef.getThriftRef(),
					new AsyncMethodCallback<createMixerEndPoint_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@Override
						public void onComplete(createMixerEndPoint_call response) {
							MediaElementRefDTO endPointRef;
							try {
								endPointRef = new MediaElementRefDTO(response
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
							MediaElement endPoint = (MediaElement) ctx.getBean(
									"mediaObject", endPointRef);
							cont.onSuccess(endPoint);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

	protected void createEndPoint(Command params,
			final Continuation<MediaElement> cont)
			throws KurentoMediaFrameworkException {

		final AsyncClient client = clientPool.acquireAsync();

		try {
			client.createMixerEndPointWithParams(
					objectRef.getThriftRef(),
					params,
					new AsyncMethodCallback<createMixerEndPointWithParams_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@Override
						public void onComplete(
								createMixerEndPointWithParams_call response) {
							MediaElementRefDTO endPointRef;
							try {
								endPointRef = new MediaElementRefDTO(response
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
							MediaElement endPoint = (MediaElement) ctx.getBean(
									"mediaObject", endPointRef);
							cont.onSuccess(endPoint);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
	}

}
