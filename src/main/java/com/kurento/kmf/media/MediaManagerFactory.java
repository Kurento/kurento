package com.kurento.kmf.media;

import java.io.IOException;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kmf.media.internal.MediaServerServiceManager;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MediaServerService.AsyncClient.createMediaFactory_call;

public class MediaManagerFactory {

	private final MediaServerServiceManager serviceManager;

	MediaManagerFactory(String address, int port, MediaManagerHandler listener)
			throws IOException {
		MediaServerServiceManager.init(address, port, listener);
		serviceManager = MediaServerServiceManager.getInstance();
	}

	public MediaManager createMediaManager() throws MediaException {
		try {
			MediaServerService.Client service = serviceManager
					.getMediaServerService();
			// TODO: Register to receive callbacks
			com.kurento.kms.api.MediaObject mediaManager = service
					.createMediaFactory();
			serviceManager.releaseMediaServerService(service);
			return new MediaManager(mediaManager);
		} catch (MediaServerException e) {
			throw new MediaException(e.getMessage(), e);
		} catch (TException e) {
			throw new MediaException(e.getMessage(), e);
		}
	}

	public void createMediaManager(final Continuation<MediaManager> cont)
			throws IOException {
		try {
			MediaServerService.AsyncClient service = serviceManager
					.getMediaServerServiceAsync();
			service.createMediaFactory(new AsyncMethodCallback<MediaServerService.AsyncClient.createMediaFactory_call>() {
				@Override
				public void onComplete(createMediaFactory_call response) {
					try {
						com.kurento.kms.api.MediaObject mediaFactory = response
								.getResult();
						cont.onSuccess(new MediaManager(mediaFactory));
					} catch (MediaServerException e) {
						cont.onError(new RuntimeException(e.getMessage(), e));
					} catch (TException e) {
						cont.onError(new IOException(e.getMessage(), e));
					}
				}

				@Override
				public void onError(Exception exception) {
					cont.onError(exception);
				}
			});
			serviceManager.releaseMediaServerServiceAsync(service);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}
}
