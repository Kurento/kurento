package com.kurento.kmf.media;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kmf.media.internal.MediaServerServiceManager;
import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MediaServerService.AsyncClient.getMediaSinksByMediaType_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.getMediaSinks_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.getMediaSrcsByMediaType_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.getMediaSrcs_call;
import com.kurento.kms.api.MediaType;

public abstract class MediaElement extends MediaObject {

	private static final long serialVersionUID = 1L;

	MediaElement(com.kurento.kms.api.MediaObject mediaElement) {
		super(mediaElement);
	}

	/* SYNC */

	/**
	 * Send a command to an element
	 * 
	 * @param command
	 *            Command in string format that the element should understand
	 * @return The result of the command execution
	 */
	String sendCommand(String command) {
		throw new NotImplementedException();
		// TODO: Implement
	}

	/**
	 * 
	 * @return the MediaManager parent of this MediaElement
	 */
	public MediaManager getMediaManager() {
		// TODO: implement using getParent method
		throw new NotImplementedException();
	}

	public Collection<MediaSrc> getMediaSrcs() throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			List<com.kurento.kms.api.MediaObject> tMediaSrcs = service
					.getMediaSrcs(mediaObject);
			List<MediaSrc> mediaSrcs = new ArrayList<MediaSrc>();
			for (com.kurento.kms.api.MediaObject tms : tMediaSrcs) {
				mediaSrcs.add(new MediaSrc(tms));
			}
			manager.releaseMediaServerService(service);
			return mediaSrcs;
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public Collection<MediaSink> getMediaSinks() throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			List<com.kurento.kms.api.MediaObject> tMediaSinks = service
					.getMediaSinks(mediaObject);
			List<MediaSink> mediaSinks = new ArrayList<MediaSink>();
			for (com.kurento.kms.api.MediaObject tms : tMediaSinks) {
				mediaSinks.add(new MediaSink(tms));
			}
			manager.releaseMediaServerService(service);
			return mediaSinks;
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public Collection<MediaSrc> getMediaSrcs(MediaType mediaType)
			throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			List<com.kurento.kms.api.MediaObject> tMediaSrcs = service
					.getMediaSrcsByMediaType(mediaObject, mediaType);
			List<MediaSrc> mediaSrcs = new ArrayList<MediaSrc>();
			for (com.kurento.kms.api.MediaObject tms : tMediaSrcs) {
				mediaSrcs.add(new MediaSrc(tms));
			}
			manager.releaseMediaServerService(service);
			return mediaSrcs;
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public Collection<MediaSink> getMediaSinks(MediaType mediaType)
			throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			List<com.kurento.kms.api.MediaObject> tMediaSinks = service
					.getMediaSinksByMediaType(mediaObject, mediaType);
			List<MediaSink> mediaSinks = new ArrayList<MediaSink>();
			for (com.kurento.kms.api.MediaObject tms : tMediaSinks) {
				mediaSinks.add(new MediaSink(tms));
			}
			manager.releaseMediaServerService(service);
			return mediaSinks;
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	/* ASYNC */

	/**
	 * Send a command to an element
	 * 
	 * @param command
	 *            Command in string format that the element should understand
	 * @param cont
	 *            A callback to receive the result of the execution
	 */
	void sendCommand(String command, final Continuation<String> cont) {
		throw new NotImplementedException();
		// TODO: Implement using getParent method
	}

	/**
	 * 
	 * @return the MediaManager parent of this MediaElement
	 */
	public void getMediaManager(final Continuation<MediaManager> cont) {
		// TODO: implement
		throw new NotImplementedException();
	}

	public void getMediaSrcs(final Continuation<Collection<MediaSrc>> cont)
			throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.AsyncClient service = manager
					.getMediaServerServiceAsync();
			service.getMediaSrcs(
					mediaObject,
					new AsyncMethodCallback<MediaServerService.AsyncClient.getMediaSrcs_call>() {
						@Override
						public void onComplete(getMediaSrcs_call response) {
							try {
								List<com.kurento.kms.api.MediaObject> tMediaSrcs = response
										.getResult();
								List<MediaSrc> mediaSrcs = new ArrayList<MediaSrc>();
								for (com.kurento.kms.api.MediaObject tms : tMediaSrcs) {
									mediaSrcs.add(new MediaSrc(tms));
								}
								cont.onSuccess(mediaSrcs);
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

	public void getMediaSinks(final Continuation<Collection<MediaSink>> cont)
			throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.AsyncClient service = manager
					.getMediaServerServiceAsync();
			service.getMediaSinks(
					mediaObject,
					new AsyncMethodCallback<MediaServerService.AsyncClient.getMediaSinks_call>() {
						@Override
						public void onComplete(getMediaSinks_call response) {
							try {
								List<com.kurento.kms.api.MediaObject> tMediaSrcs = response
										.getResult();
								List<MediaSrc> mediaSrcs = new ArrayList<MediaSrc>();
								for (com.kurento.kms.api.MediaObject tms : tMediaSrcs) {
									mediaSrcs.add(new MediaSrc(tms));
								}

								List<com.kurento.kms.api.MediaObject> tMediaSinks = response
										.getResult();
								List<MediaSink> mediaSinks = new ArrayList<MediaSink>();
								for (com.kurento.kms.api.MediaObject tms : tMediaSinks) {
									mediaSinks.add(new MediaSink(tms));
								}
								cont.onSuccess(mediaSinks);
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

	public void getMediaSrcs(MediaType mediaType,
			final Continuation<Collection<MediaSrc>> cont) throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.AsyncClient service = manager
					.getMediaServerServiceAsync();
			service.getMediaSrcsByMediaType(
					mediaObject,
					mediaType,
					new AsyncMethodCallback<MediaServerService.AsyncClient.getMediaSrcsByMediaType_call>() {
						@Override
						public void onComplete(
								getMediaSrcsByMediaType_call response) {
							try {
								List<com.kurento.kms.api.MediaObject> tMediaSrcs = response
										.getResult();
								List<MediaSrc> mediaSrcs = new ArrayList<MediaSrc>();
								for (com.kurento.kms.api.MediaObject tms : tMediaSrcs) {
									mediaSrcs.add(new MediaSrc(tms));
								}
								cont.onSuccess(mediaSrcs);
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

	public void getMediaSinks(MediaType mediaType,
			final Continuation<Collection<MediaSink>> cont) throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.AsyncClient service = manager
					.getMediaServerServiceAsync();
			service.getMediaSinksByMediaType(
					mediaObject,
					mediaType,
					new AsyncMethodCallback<MediaServerService.AsyncClient.getMediaSinksByMediaType_call>() {
						@Override
						public void onComplete(
								getMediaSinksByMediaType_call response) {
							try {
								List<com.kurento.kms.api.MediaObject> tMediaSrcs = response
										.getResult();
								List<MediaSrc> mediaSrcs = new ArrayList<MediaSrc>();
								for (com.kurento.kms.api.MediaObject tms : tMediaSrcs) {
									mediaSrcs.add(new MediaSrc(tms));
								}

								List<com.kurento.kms.api.MediaObject> tMediaSinks = response
										.getResult();
								List<MediaSink> mediaSinks = new ArrayList<MediaSink>();
								for (com.kurento.kms.api.MediaObject tms : tMediaSinks) {
									mediaSinks.add(new MediaSink(tms));
								}
								cont.onSuccess(mediaSinks);
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
