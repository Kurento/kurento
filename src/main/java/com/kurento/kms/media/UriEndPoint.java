package com.kurento.kms.media;

import java.io.IOException;

import org.apache.commons.lang.NotImplementedException;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kms.api.MediaObject;
import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MediaServerService.AsyncClient.pausePlayer_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.stopPlayer_call;
import com.kurento.kms.media.internal.MediaServerServiceManager;

public class UriEndPoint extends EndPoint {

	private static final long serialVersionUID = 1L;

	UriEndPoint(MediaObject mediaStream) {
		super(mediaStream);
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

	void pause() throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			service.pausePlayer(mediaObject);
			manager.releaseMediaServerService(service);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	void stop() throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			service.stopPlayer(mediaObject);
			manager.releaseMediaServerService(service);
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
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.AsyncClient service = manager
					.getMediaServerServiceAsync();
			service.pausePlayer(
					mediaObject,
					new AsyncMethodCallback<MediaServerService.AsyncClient.pausePlayer_call>() {
						@Override
						public void onComplete(pausePlayer_call response) {
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
			manager.releaseMediaServerServiceAsync(service);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public void stop(final Continuation<Void> cont) throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.AsyncClient service = manager
					.getMediaServerServiceAsync();
			service.stopPlayer(
					mediaObject,
					new AsyncMethodCallback<MediaServerService.AsyncClient.stopPlayer_call>() {
						@Override
						public void onComplete(stopPlayer_call response) {
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
			manager.releaseMediaServerServiceAsync(service);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}
}
