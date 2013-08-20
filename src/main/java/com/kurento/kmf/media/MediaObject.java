package com.kurento.kmf.media;

import java.io.IOException;
import java.io.Serializable;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.kurento.kms.api.MediaEvent;
import com.kurento.kms.api.MediaObjectId;
import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MediaServerService.AsyncClient.getParent_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.release_call;
import com.kurento.kms.api.NoParentException;

public abstract class MediaObject implements Serializable {

	private static final long serialVersionUID = 1L;

	@Autowired
	protected MediaServerServiceManager mssm;

	@Autowired
	protected MediaServerHandler handler;

	@Autowired
	protected ApplicationContext applicationContext;

	@Autowired
	protected MediaManagerFactory mediaManagerFactory;

	protected MediaObjectId mediaObjectId;

	MediaObject(MediaObjectId mediaObjectId) {
		this.mediaObjectId = mediaObjectId;
	}

	/* SYNC */

	public MediaObject getParent() throws IOException {
		MediaServerService.Client service = mssm.getMediaServerService();

		try {
			return mediaManagerFactory.getMediaObject(service
					.getParent(mediaObjectId));
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (NoParentException e) {
			return null;
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			mssm.releaseMediaServerService(service);
		}
	}

	public void release() throws IOException {
		MediaServerService.Client service = mssm.getMediaServerService();

		try {
			service.release(mediaObjectId);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			mssm.releaseMediaServerService(service);
			delete();
		}
	}

	/* ASYNC */

	public void getParent(final Continuation<MediaObject> cont)
			throws IOException {
		MediaServerService.AsyncClient service = mssm
				.getMediaServerServiceAsync();

		try {
			service.getParent(
					mediaObjectId,
					new AsyncMethodCallback<MediaServerService.AsyncClient.getParent_call>() {
						@Override
						public void onComplete(getParent_call response) {
							try {
								MediaObject parent = mediaManagerFactory
										.getMediaObject(response.getResult());
								cont.onSuccess(parent);
							} catch (MediaObjectNotFoundException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (MediaServerException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (NoParentException e) {
								cont.onSuccess(null);
							} catch (TException e) {
								cont.onError(new IOException(e.getMessage(), e));
							}
						}

						@Override
						public void onError(Exception exception) {
							cont.onError(exception);
						}
					});
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			mssm.releaseMediaServerServiceAsync(service);
			delete();
		}
	}

	public void release(final Continuation<Void> cont) throws IOException {
		MediaServerService.AsyncClient service = mssm
				.getMediaServerServiceAsync();

		try {
			service.release(
					mediaObjectId,
					new AsyncMethodCallback<MediaServerService.AsyncClient.release_call>() {
						@Override
						public void onComplete(release_call response) {
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
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			mssm.releaseMediaServerServiceAsync(service);
		}
	}

	KmsEvent deserializeEvent(MediaEvent event) {
		throw new UnsupportedOperationException(
				"Cannot deserialize events on instances of "
						+ this.getClass().getSimpleName());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj.getClass().equals(this.getClass())) {
			return false;
		} else {
			MediaObject mo = (MediaObject) obj;
			return mo.mediaObjectId.getId() == this.mediaObjectId.getId();
		}
	}

	@Override
	public int hashCode() {
		return Long.valueOf(mediaObjectId.getId()).hashCode();
	}

	// TODO: we should implement a mechanism guaranteeing that this method and
	// at least one of the release methods are called when MediaObject is not
	// always reachable
	protected void delete() {
		handler.removeAllListeners(this);
	}
}
