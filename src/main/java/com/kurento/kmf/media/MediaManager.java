package com.kurento.kmf.media;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kmf.media.internal.MediaServerServiceManager;
import com.kurento.kms.api.FilterType;
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

	MediaManager(com.kurento.kms.api.MediaObject mediaManager) {
		super(mediaManager);
	}

	private static <T extends MediaObject> T createInstance(Class<T> type,
			com.kurento.kms.api.MediaObject mediaObject) {
		try {
			Constructor<T> constructor = type
					.getDeclaredConstructor(com.kurento.kms.api.MediaObject.class);
			return constructor.newInstance(mediaObject);
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
			com.kurento.kms.api.MediaObject sdpEndPoint;
			if (sdp == null) {
				sdpEndPoint = service.createSdpEndPoint(mediaObject, t);
			} else {
				sdpEndPoint = service.createSdpEndPointWithFixedSdp(
						mediaObject, t, sdp);
			}
			return createInstance(type, sdpEndPoint);
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
			com.kurento.kms.api.MediaObject uriEndPoint = service
					.createUriEndPoint(mediaObject, t, uri);
			return createInstance(type, uriEndPoint);
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
			com.kurento.kms.api.MediaObject httpEndPoint = service
					.createHttpEndPoint(mediaObject);
			return new HttpEndPoint(httpEndPoint);
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
			com.kurento.kms.api.MediaObject mixer = service.createMixer(
					mediaObject, t);
			return createInstance(type, mixer);
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
			com.kurento.kms.api.MediaObject filter = service.createFilter(
					mediaObject, t);
			return createInstance(type, filter);
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
						mediaObject,
						t,
						new AsyncMethodCallback<MediaServerService.AsyncClient.createSdpEndPoint_call>() {
							@Override
							public void onComplete(
									createSdpEndPoint_call response) {
								try {
									com.kurento.kms.api.MediaObject sdpEndPoint = response
											.getResult();
									cont.onSuccess(createInstance(type,
											sdpEndPoint));
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
						mediaObject,
						t,
						sdp,
						new AsyncMethodCallback<MediaServerService.AsyncClient.createSdpEndPointWithFixedSdp_call>() {
							@Override
							public void onComplete(
									createSdpEndPointWithFixedSdp_call response) {
								try {
									com.kurento.kms.api.MediaObject sdpEndPoint = response
											.getResult();
									cont.onSuccess(createInstance(type,
											sdpEndPoint));
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
					mediaObject,
					t,
					uri,
					new AsyncMethodCallback<MediaServerService.AsyncClient.createUriEndPoint_call>() {
						@Override
						public void onComplete(createUriEndPoint_call response) {
							try {
								com.kurento.kms.api.MediaObject uriEndPoint = response
										.getResult();
								cont.onSuccess(createInstance(type, uriEndPoint));
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
					mediaObject,
					new AsyncMethodCallback<MediaServerService.AsyncClient.createHttpEndPoint_call>() {
						@Override
						public void onComplete(createHttpEndPoint_call response) {
							try {
								com.kurento.kms.api.MediaObject httpEndPoint = response
										.getResult();
								cont.onSuccess(new HttpEndPoint(httpEndPoint));
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
					mediaObject,
					t,
					new AsyncMethodCallback<MediaServerService.AsyncClient.createFilter_call>() {
						@Override
						public void onComplete(createFilter_call response) {
							try {
								com.kurento.kms.api.MediaObject filter = response
										.getResult();
								cont.onSuccess(createInstance(type, filter));
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
					mediaObject,
					t,
					new AsyncMethodCallback<MediaServerService.AsyncClient.createMixer_call>() {
						@Override
						public void onComplete(createMixer_call response) {
							try {
								com.kurento.kms.api.MediaObject mixer = response
										.getResult();
								cont.onSuccess(createInstance(type, mixer));
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
