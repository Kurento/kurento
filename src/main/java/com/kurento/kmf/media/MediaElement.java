package com.kurento.kmf.media;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kms.api.Command;
import com.kurento.kms.api.CommandResult;
import com.kurento.kms.api.EncodingException;
import com.kurento.kms.api.MediaObjectId;
import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MediaServerService.AsyncClient.getMediaSinksByMediaType_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.getMediaSinks_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.getMediaSrcsByMediaType_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.getMediaSrcs_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.sendCommand_call;
import com.kurento.kms.api.MediaType;

public abstract class MediaElement extends MediaObject {

	private static final long serialVersionUID = 1L;

	MediaElement(MediaObjectId mediaElementId) {
		super(mediaElementId);
	}

	/* SYNC */

	/**
	 * Send a command to an element
	 * 
	 * @param command
	 * 
	 * @return The result of the command execution
	 */
	CommandResult sendCommand(Command command) throws IOException {
		MediaServerService.Client service = mssm.getMediaServerService();

		try {
			return service.sendCommand(mediaObjectId, command);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			mssm.releaseMediaServerService(service);
		}
	}

	/**
	 * 
	 * @return the MediaManager parent of this MediaElement
	 * @throws IOException
	 */
	public MediaManager getMediaManager() throws IOException {
		// FIXME: This function fails for MixerEndPoints (it returns null
		// instead of the MediaManager)
		MediaObject parent = getParent();
		if (parent instanceof MediaManager) {
			return (MediaManager) parent;
		}
		return null;
	}

	public Collection<MediaSrc> getMediaSrcs() throws IOException {
		MediaServerService.Client service = mssm.getMediaServerService();

		try {
			List<MediaObjectId> tMediaSrcs = service
					.getMediaSrcs(mediaObjectId);
			List<MediaSrc> mediaSrcs = new ArrayList<MediaSrc>();
			for (MediaObjectId tms : tMediaSrcs) {
				mediaSrcs.add((MediaSrc) applicationContext.getBean(
						"mediaObject", MediaSrc.class, tms));
			}

			return mediaSrcs;
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			mssm.releaseMediaServerService(service);
		}
	}

	public Collection<MediaSink> getMediaSinks() throws IOException {
		MediaServerService.Client service = mssm.getMediaServerService();

		try {
			List<MediaObjectId> tMediaSinks = service
					.getMediaSinks(mediaObjectId);
			List<MediaSink> mediaSinks = new ArrayList<MediaSink>();
			for (MediaObjectId tms : tMediaSinks) {
				mediaSinks.add((MediaSink) applicationContext.getBean(
						"mediaObject", MediaSink.class, tms));
			}

			return mediaSinks;
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			mssm.releaseMediaServerService(service);
		}
	}

	public Collection<MediaSrc> getMediaSrcs(MediaType mediaType)
			throws IOException {
		MediaServerService.Client service = mssm.getMediaServerService();

		try {
			List<MediaObjectId> tMediaSrcs = service.getMediaSrcsByMediaType(
					mediaObjectId, mediaType);
			List<MediaSrc> mediaSrcs = new ArrayList<MediaSrc>();
			for (MediaObjectId tms : tMediaSrcs) {
				mediaSrcs.add((MediaSrc) applicationContext.getBean(
						"mediaObject", MediaSrc.class, tms));
			}

			return mediaSrcs;
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			mssm.releaseMediaServerService(service);
		}
	}

	public Collection<MediaSink> getMediaSinks(MediaType mediaType)
			throws IOException {
		MediaServerService.Client service = mssm.getMediaServerService();

		try {
			List<MediaObjectId> tMediaSinks = service.getMediaSinksByMediaType(
					mediaObjectId, mediaType);
			List<MediaSink> mediaSinks = new ArrayList<MediaSink>();
			for (MediaObjectId tms : tMediaSinks) {
				mediaSinks.add((MediaSink) applicationContext.getBean(
						"mediaObject", MediaSink.class, tms));
			}

			return mediaSinks;
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			mssm.releaseMediaServerService(service);
		}
	}

	/* ASYNC */

	/**
	 * Send a command to an element
	 * 
	 * @param command
	 * @param cont
	 *            A callback to receive the result of the execution
	 */
	void sendCommand(Command command, final Continuation<String> cont)
			throws IOException {
		MediaServerService.AsyncClient service = mssm
				.getMediaServerServiceAsync();

		try {
			service.sendCommand(
					mediaObjectId,
					command,
					new AsyncMethodCallback<MediaServerService.AsyncClient.sendCommand_call>() {
						@Override
						public void onComplete(sendCommand_call response) {
							try {
								response.getResult();
							} catch (MediaObjectNotFoundException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (MediaServerException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (EncodingException e) {
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

	/**
	 * 
	 * @return the MediaManager parent of this MediaElement
	 * @throws IOException
	 */
	public void getMediaManager(final Continuation<MediaManager> cont)
			throws IOException {
		getParent(new Continuation<MediaObject>() {
			@Override
			public void onSuccess(MediaObject result) {
				if (result instanceof MediaManager) {
					cont.onSuccess((MediaManager) result);
				} else {
					cont.onSuccess(null);
				}
			}

			@Override
			public void onError(Throwable cause) {
				cont.onError(cause);
			}
		});
	}

	public void getMediaSrcs(final Continuation<Collection<MediaSrc>> cont)
			throws IOException {
		MediaServerService.AsyncClient service = mssm
				.getMediaServerServiceAsync();

		try {
			service.getMediaSrcs(
					mediaObjectId,
					new AsyncMethodCallback<MediaServerService.AsyncClient.getMediaSrcs_call>() {
						@Override
						public void onComplete(getMediaSrcs_call response) {
							try {
								List<MediaObjectId> tMediaSrcs = response
										.getResult();
								List<MediaSrc> mediaSrcs = new ArrayList<MediaSrc>();
								for (MediaObjectId tms : tMediaSrcs) {
									mediaSrcs.add((MediaSrc) applicationContext
											.getBean("mediaObject",
													MediaSrc.class, tms));
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
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			mssm.releaseMediaServerServiceAsync(service);
		}
	}

	public void getMediaSinks(final Continuation<Collection<MediaSink>> cont)
			throws IOException {
		MediaServerService.AsyncClient service = mssm
				.getMediaServerServiceAsync();

		try {
			service.getMediaSinks(
					mediaObjectId,
					new AsyncMethodCallback<MediaServerService.AsyncClient.getMediaSinks_call>() {
						@Override
						public void onComplete(getMediaSinks_call response) {
							try {
								List<MediaObjectId> tMediaSinks = response
										.getResult();
								List<MediaSink> mediaSinks = new ArrayList<MediaSink>();
								for (MediaObjectId tms : tMediaSinks) {
									mediaSinks
											.add((MediaSink) applicationContext
													.getBean("mediaObject",
															MediaSink.class,
															tms));
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
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			mssm.releaseMediaServerServiceAsync(service);
		}
	}

	public void getMediaSrcs(MediaType mediaType,
			final Continuation<Collection<MediaSrc>> cont) throws IOException {
		MediaServerService.AsyncClient service = mssm
				.getMediaServerServiceAsync();

		try {
			service.getMediaSrcsByMediaType(
					mediaObjectId,
					mediaType,
					new AsyncMethodCallback<MediaServerService.AsyncClient.getMediaSrcsByMediaType_call>() {
						@Override
						public void onComplete(
								getMediaSrcsByMediaType_call response) {
							try {
								List<MediaObjectId> tMediaSrcs = response
										.getResult();
								List<MediaSrc> mediaSrcs = new ArrayList<MediaSrc>();
								for (MediaObjectId tms : tMediaSrcs) {
									mediaSrcs.add((MediaSrc) applicationContext
											.getBean("mediaObject",
													MediaSrc.class, tms));
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
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			mssm.releaseMediaServerServiceAsync(service);
		}
	}

	public void getMediaSinks(MediaType mediaType,
			final Continuation<Collection<MediaSink>> cont) throws IOException {
		MediaServerService.AsyncClient service = mssm
				.getMediaServerServiceAsync();

		try {
			service.getMediaSinksByMediaType(
					mediaObjectId,
					mediaType,
					new AsyncMethodCallback<MediaServerService.AsyncClient.getMediaSinksByMediaType_call>() {
						@Override
						public void onComplete(
								getMediaSinksByMediaType_call response) {
							try {
								List<MediaObjectId> tMediaSinks = response
										.getResult();
								List<MediaSink> mediaSinks = new ArrayList<MediaSink>();
								for (MediaObjectId tms : tMediaSinks) {
									mediaSinks
											.add((MediaSink) applicationContext
													.getBean("mediaObject",
															MediaSink.class,
															tms));
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
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			mssm.releaseMediaServerServiceAsync(service);
		}
	}

}
