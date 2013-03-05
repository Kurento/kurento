package com.kurento.kms.media;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kms.api.JoinException;
import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MediaServerService.AsyncClient.getMediaSinksByMediaType_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.getMediaSinks_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.getMediaSrcsByMediaType_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.getMediaSrcs_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.join_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.unjoin_call;
import com.kurento.kms.api.MediaType;
import com.kurento.kms.media.internal.MediaServerServiceManager;

public abstract class Joinable extends MediaObject {

	private static final long serialVersionUID = 1L;

	Joinable(com.kurento.kms.api.MediaObject joinable) {
		super(joinable);
	}

	public void join(Joinable peer, final Continuation<Void> cont)
			throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.AsyncClient service = manager
					.getMediaServerServiceAsync();
			service.join(
					mediaObject,
					peer.mediaObject,
					new AsyncMethodCallback<MediaServerService.AsyncClient.join_call>() {
						@Override
						public void onComplete(join_call response) {
							try {
								response.getResult();
								cont.onSuccess(null);
							} catch (MediaObjectNotFoundException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (JoinException e) {
								cont.onError(new MediaException(e.getMessage(),
										e));
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

	public void unjoin(Joinable peer, final Continuation<Void> cont)
			throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.AsyncClient service = manager
					.getMediaServerServiceAsync();
			service.unjoin(
					mediaObject,
					peer.mediaObject,
					new AsyncMethodCallback<MediaServerService.AsyncClient.unjoin_call>() {
						@Override
						public void onComplete(unjoin_call response) {
							try {
								response.getResult();
								cont.onSuccess(null);
							} catch (MediaObjectNotFoundException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (JoinException e) {
								cont.onError(new MediaException(e.getMessage(),
										e));
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
