package com.kurento.kmf.media;

import java.io.IOException;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kms.api.MediaObjectId;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MediaServerService.AsyncClient.createMediaManager_call;

public class MediaManagerFactory {

	private final int handlerId;

	MediaManagerFactory(String serverAddress, int serverPort,
			MediaManagerHandler handler, int handlerId, String handlerAddress,
			int handlerPort) throws IOException {
		this.handlerId = handlerId;
		MediaServerServiceManager.init(serverAddress, serverPort, handler,
				handlerId, handlerAddress, handlerPort);
	}

	public void destroy() {
		MediaServerServiceManager.destroy();
	}

	/* SYNC */

	public MediaManager createMediaManager() throws MediaException {
		try {
			MediaServerService.Client service = MediaServerServiceManager
					.getMediaServerService();
			MediaObjectId mediaManagerId = service
					.createMediaManager(handlerId);
			MediaServerServiceManager.releaseMediaServerService(service);
			return new MediaManager(mediaManagerId);
		} catch (MediaServerException e) {
			throw new MediaException(e.getMessage(), e);
		} catch (TException e) {
			throw new MediaException(e.getMessage(), e);
		} catch (IOException e) {
			throw new MediaException(e.getMessage(), e);
		}
	}

	/* ASYNC */

	public void createMediaManager(final Continuation<MediaManager> cont)
			throws IOException {
		try {
			MediaServerService.AsyncClient service = MediaServerServiceManager
					.getMediaServerServiceAsync();
			service.createMediaManager(
					handlerId,
					new AsyncMethodCallback<MediaServerService.AsyncClient.createMediaManager_call>() {
						@Override
						public void onComplete(createMediaManager_call response) {
							try {
								MediaObjectId mediaFactoryId = response
										.getResult();
								cont.onSuccess(new MediaManager(mediaFactoryId));
							} catch (MediaServerException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (TException e) {
								cont.onError(new IOException(e.getMessage(), e));
							}
						}

						@Override
						public void onError(Exception exception) {
							cont.onError(exception);
						}
					});
			MediaServerServiceManager.releaseMediaServerServiceAsync(service);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}
}
