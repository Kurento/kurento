package com.kurento.kmf.media;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kms.api.FilterType;
import com.kurento.kms.api.MediaObjectId;
import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MediaServerService.AsyncClient.createFilter_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.createHttpEndPoint_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.createMixer_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.createSdpEndPointWithFixedSdp_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.createSdpEndPoint_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.createUriEndPoint_call;
import com.kurento.kms.api.MixerType;
import com.kurento.kms.api.SdpEndPointType;
import com.kurento.kms.api.UriEndPointType;

public class MediaManager extends MediaObject {

	private static final long serialVersionUID = 1L;

	MediaManager(MediaObjectId mediaManagerId) {
		super(mediaManagerId);
	}

	private static <T extends MediaObject> T createInstance(Class<T> type,
			MediaObjectId mediaObjectId) {
		try {
			Constructor<T> constructor = type
					.getDeclaredConstructor(MediaObjectId.class);
			return constructor.newInstance(mediaObjectId);
		} catch (SecurityException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(e);
		} catch (InstantiationException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/* SYNC */

	public <T extends SdpEndPoint> T createSdpEndPoint(Class<T> type)
			throws IOException {
		return createSdpEndPoint(type, (String) null);
	}

	public <T extends SdpEndPoint> T createSdpEndPoint(Class<T> type, String sdp)
			throws IOException {
		SdpEndPointType t = SdpEndPoint.getType(type);
		MediaServerService.Client service = MediaServerServiceManager
				.getMediaServerService();

		try {
			MediaObjectId sdpEndPointId;
			if (sdp == null) {
				sdpEndPointId = service.createSdpEndPoint(mediaObjectId, t);
			} else {
				sdpEndPointId = service.createSdpEndPointWithFixedSdp(
						mediaObjectId, t, sdp);
			}
			return createInstance(type, sdpEndPointId);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			MediaServerServiceManager.releaseMediaServerService(service);
		}
	}

	public <T extends UriEndPoint> T createUriEndPoint(Class<T> type, String uri)
			throws MediaException, IOException {
		UriEndPointType t = UriEndPoint.getType(type);
		MediaServerService.Client service = MediaServerServiceManager
				.getMediaServerService();

		try {
			MediaObjectId uriEndPointId = service
					.createUriEndPoint(mediaObjectId, t, uri);
			return createInstance(type, uriEndPointId);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			MediaServerServiceManager.releaseMediaServerService(service);
		}
	}

	public HttpEndPoint createHttpEndPoint() throws MediaException, IOException {
		MediaServerService.Client service = MediaServerServiceManager
				.getMediaServerService();

		try {
			MediaObjectId httpEndPointId = service
					.createHttpEndPoint(mediaObjectId);
			return new HttpEndPoint(httpEndPointId);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			MediaServerServiceManager.releaseMediaServerService(service);
		}
	}

	public <T extends Mixer> T createMixer(Class<T> type)
			throws MediaException, IOException {
		MixerType t = Mixer.getType(type);
		MediaServerService.Client service = MediaServerServiceManager
				.getMediaServerService();

		try {
			MediaObjectId mixerId = service.createMixer(
					mediaObjectId, t);
			return createInstance(type, mixerId);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			MediaServerServiceManager.releaseMediaServerService(service);
		}
	}

	public <T extends Filter> T createFilter(Class<T> type)
			throws MediaException, IOException {
		FilterType t = Filter.getType(type);
		MediaServerService.Client service = MediaServerServiceManager
				.getMediaServerService();

		try {
			MediaObjectId filterId = service.createFilter(
					mediaObjectId, t);
			return createInstance(type, filterId);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			MediaServerServiceManager.releaseMediaServerService(service);
		}
	}

	/* ASYNC */

	public <T extends SdpEndPoint> void createSdpEndPoint(final Class<T> type,
			final Continuation<T> cont) throws IOException {
		createSdpEndPoint(type, null, cont);
	}

	public <T extends SdpEndPoint> void createSdpEndPoint(final Class<T> type,
			String sdp, final Continuation<T> cont) throws IOException {
		SdpEndPointType t = SdpEndPoint.getType(type);
		MediaServerService.AsyncClient service = MediaServerServiceManager
				.getMediaServerServiceAsync();

		try {
			if (sdp == null) {
				service.createSdpEndPoint(
						mediaObjectId,
						t,
						new AsyncMethodCallback<MediaServerService.AsyncClient.createSdpEndPoint_call>() {
							@Override
							public void onComplete(
									createSdpEndPoint_call response) {
								try {
									MediaObjectId sdpEndPointId = response
											.getResult();
									cont.onSuccess(createInstance(type,
											sdpEndPointId));
								} catch (MediaObjectNotFoundException e) {
									cont.onError(new RuntimeException(e
											.getMessage(), e));
								} catch (MediaServerException e) {
									cont.onError(new RuntimeException(e
											.getMessage(), e));
								} catch (TException e) {
									cont.onError(new IOException(
											e.getMessage(), e));
								}
							}

							@Override
							public void onError(Exception exception) {
								cont.onError(exception);
							}
						});
			} else {
				service.createSdpEndPointWithFixedSdp(
						mediaObjectId,
						t,
						sdp,
						new AsyncMethodCallback<MediaServerService.AsyncClient.createSdpEndPointWithFixedSdp_call>() {
							@Override
							public void onComplete(
									createSdpEndPointWithFixedSdp_call response) {
								try {
									MediaObjectId sdpEndPointId = response
											.getResult();
									cont.onSuccess(createInstance(type,
											sdpEndPointId));
								} catch (MediaObjectNotFoundException e) {
									cont.onError(new RuntimeException(e
											.getMessage(), e));
								} catch (MediaServerException e) {
									cont.onError(new RuntimeException(e
											.getMessage(), e));
								} catch (TException e) {
									cont.onError(new IOException(
											e.getMessage(), e));
								}
							}

							@Override
							public void onError(Exception exception) {
								cont.onError(exception);
							}
						});
			}
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			MediaServerServiceManager.releaseMediaServerServiceAsync(service);
		}
	}

	public <T extends UriEndPoint> void createUriEndPoint(final Class<T> type,
			String uri, final Continuation<T> cont) throws IOException {
		UriEndPointType t = UriEndPoint.getType(type);
		MediaServerService.AsyncClient service = MediaServerServiceManager
				.getMediaServerServiceAsync();

		try {
			service.createUriEndPoint(
					mediaObjectId,
					t,
					uri,
					new AsyncMethodCallback<MediaServerService.AsyncClient.createUriEndPoint_call>() {
						@Override
						public void onComplete(createUriEndPoint_call response) {
							try {
								MediaObjectId uriEndPointId = response
										.getResult();
								cont.onSuccess(createInstance(type, uriEndPointId));
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
			MediaServerServiceManager.releaseMediaServerServiceAsync(service);
		}
	}

	public void createHttpEndPoint(final Continuation<HttpEndPoint> cont)
			throws IOException {
		MediaServerService.AsyncClient service = MediaServerServiceManager
				.getMediaServerServiceAsync();

		try {
			service.createHttpEndPoint(
					mediaObjectId,
					new AsyncMethodCallback<MediaServerService.AsyncClient.createHttpEndPoint_call>() {
						@Override
						public void onComplete(createHttpEndPoint_call response) {
							try {
								MediaObjectId httpEndPointId = response
										.getResult();
								cont.onSuccess(new HttpEndPoint(httpEndPointId));
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
			MediaServerServiceManager.releaseMediaServerServiceAsync(service);
		}
	}

	public <T extends Filter> void createFilter(final Class<T> type,
			final Continuation<T> cont) throws IOException {
		FilterType t = Filter.getType(type);
		MediaServerService.AsyncClient service = MediaServerServiceManager
				.getMediaServerServiceAsync();

		try {
			service.createFilter(
					mediaObjectId,
					t,
					new AsyncMethodCallback<MediaServerService.AsyncClient.createFilter_call>() {
						@Override
						public void onComplete(createFilter_call response) {
							try {
								MediaObjectId filterId = response
										.getResult();
								cont.onSuccess(createInstance(type, filterId));
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
			MediaServerServiceManager.releaseMediaServerServiceAsync(service);
		}
	}

	public <T extends Mixer> void createMixer(final Class<T> type,
			final Continuation<T> cont) throws IOException {
		MixerType t = Mixer.getType(type);
		MediaServerService.AsyncClient service = MediaServerServiceManager
				.getMediaServerServiceAsync();

		try {
			service.createMixer(
					mediaObjectId,
					t,
					new AsyncMethodCallback<MediaServerService.AsyncClient.createMixer_call>() {
						@Override
						public void onComplete(createMixer_call response) {
							try {
								MediaObjectId mixerId = response
										.getResult();
								cont.onSuccess(createInstance(type, mixerId));
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
			MediaServerServiceManager.releaseMediaServerServiceAsync(service);
		}
	}

}
