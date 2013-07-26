package com.kurento.kmf.media;

import java.io.IOException;

import org.apache.commons.lang.NotImplementedException;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kmf.media.internal.MediaServerServiceManager;
import com.kurento.kms.api.MediaObject;
import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MediaServerService.AsyncClient.pause_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.stop_call;

public abstract class UriEndPoint extends EndPoint {

	private static final long serialVersionUID = 1L;

	UriEndPoint(MediaObject uriEndPoint) {
		super(uriEndPoint);
	}

	/* SYNC */

	public String getUri() {
		// TODO: Implement this method
		throw new NotImplementedException();
	}

	protected void start() throws IOException {
		// TODO: Implement this method
		throw new NotImplementedException();
	}

	public void pause() throws IOException {
		MediaServerService.Client service = MediaServerServiceManager
				.getMediaServerService();

		try {
			service.pause(mediaObject);
			MediaServerServiceManager.releaseMediaServerService(service);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public void stop() throws IOException {
		MediaServerService.Client service = MediaServerServiceManager
				.getMediaServerService();

		try {
			service.stop(mediaObject);
			MediaServerServiceManager.releaseMediaServerService(service);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	/* ASYNC */

	public void getUri(Continuation<String> cont) throws IOException {
		throw new NotImplementedException();
		// TODO: Implement this method
	}

	protected void start(final Continuation<Void> cont) throws IOException {
		throw new NotImplementedException();
		// TODO: Implement this method
	}

	public void pause(final Continuation<Void> cont) throws IOException {
		MediaServerService.AsyncClient service = MediaServerServiceManager
				.getMediaServerServiceAsync();

		try {
			service.pause(
					mediaObject,
					new AsyncMethodCallback<MediaServerService.AsyncClient.pause_call>() {
						@Override
						public void onComplete(pause_call response) {
							try {
								response.getResult();
								cont.onSuccess(null);
							} catch (MediaObjectNotFoundException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
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

	public void stop(final Continuation<Void> cont) throws IOException {
		MediaServerService.AsyncClient service = MediaServerServiceManager
				.getMediaServerServiceAsync();

		try {
			service.stop(
					mediaObject,
					new AsyncMethodCallback<MediaServerService.AsyncClient.stop_call>() {
						@Override
						public void onComplete(stop_call response) {
							try {
								response.getResult();
								cont.onSuccess(null);
							} catch (MediaObjectNotFoundException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
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
