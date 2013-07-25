package com.kurento.kms.media;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang.NotImplementedException;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MediaServerService.AsyncClient.createMixer_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.createStream_call;
import com.kurento.kms.media.internal.MediaServerServiceManager;

public class MediaManager extends MediaObject {

	private static final long serialVersionUID = 1L;

	MediaManager(com.kurento.kms.api.MediaObject mediaFactory) {
		super(mediaFactory);
	}

	/* SYNC */

	public <T extends SdpEndPoint> T createSdpEndPoint(Class<T> type, String sdp)
			throws MediaException, IOException {
		throw new NotImplementedException();
		// TODO: Implement this method
	}

	public <T extends SdpEndPoint> T createSdpEndPoint(Class<T> type)
			throws MediaException, IOException {
		try {
			// TODO: Add parameter to ServiceCall
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			com.kurento.kms.api.MediaObject endPoint = service
					.createStream(mediaObject);
			manager.releaseMediaServerService(service);

			try {
				Constructor<T> constructor = type
						.getDeclaredConstructor(endPoint.getClass());
				return constructor.newInstance(endPoint);
			} catch (Exception e) {
				throw new IllegalArgumentException();
			}
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public <T extends UriEndPoint> T createUriEndPoint(Class<T> type, String uri)
			throws MediaException, IOException {
		// TODO: Implement this method
		throw new NotImplementedException();
	}

	public HttpEndPoint createHttpEndPoint() throws MediaException, IOException {
		// TODO: Implement this method
		throw new NotImplementedException();
	}

	public <T extends Mixer> T createMixer(Class<T> type) throws MediaException,
			IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();

			Field field;
			try {
				field = type.getDeclaredField(Mixer.MIXER_ID_FIELD_NAME);
			} catch (NoSuchFieldException e1) {
				throw new IllegalArgumentException();
			} catch (SecurityException e1) {
				throw new IllegalArgumentException();
			}
			com.kurento.kms.api.MediaObject mixer;
			try {
				mixer = service.createMixer(mediaObject, field.getInt(type));
			} catch (IllegalArgumentException e1) {
				throw new IllegalArgumentException();
			} catch (IllegalAccessException e1) {
				throw new IllegalArgumentException();
			}
			manager.releaseMediaServerService(service);
			try {
				Constructor<T> constructor = type.getDeclaredConstructor(mixer
						.getClass());
				return constructor.newInstance(mixer);
			} catch (Exception e) {
				throw new IllegalArgumentException();
			}
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public <T extends Filter> T createFilter(Class<T> type) throws MediaException,
			IOException {
		// TODO: Implement this method
		throw new NotImplementedException();
	}

	/* ASYNC */

	public <T extends SdpEndPoint> void createSdpEndPoint(final Class<T> type,
			String sdp, final Continuation<T> cont) throws IOException {
		// TODO: Implement this method
		throw new NotImplementedException();
	}

	public <T extends SdpEndPoint> void createSdpEndPoint(final Class<T> type,
			final Continuation<T> cont) throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.AsyncClient service = manager
					.getMediaServerServiceAsync();
			service.createStream(mediaObject,
					new AsyncMethodCallback<createStream_call>() {
						@Override
						public void onComplete(createStream_call response) {
							try {
								com.kurento.kms.api.MediaObject endPoint = response
										.getResult();

								Constructor<T> constructor = type
										.getDeclaredConstructor(endPoint
												.getClass());
								cont.onSuccess(constructor
										.newInstance(endPoint));
							} catch (MediaServerException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (TException e) {
								cont.onError(new IOException(e.getMessage(), e));
							} catch (NoSuchMethodException e) {
								cont.onError(new IllegalArgumentException());
							} catch (SecurityException e) {
								cont.onError(new IllegalArgumentException());
							} catch (InstantiationException e) {
								cont.onError(new IllegalArgumentException());
							} catch (IllegalAccessException e) {
								cont.onError(new IllegalArgumentException());
							} catch (IllegalArgumentException e) {
								cont.onError(new IllegalArgumentException());
							} catch (InvocationTargetException e) {
								cont.onError(new IllegalArgumentException());
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

	public <T extends UriEndPoint> void createUriEndPoint(final Class<T> type,
			String uri, final Continuation<T> cont) throws IOException {
		// TODO: Implement this part
		throw new NotImplementedException();
	}

	public void createHttpEndPoint(final Continuation<HttpEndPoint> cont)
			throws IOException {
		// TODO: Implement this part
		throw new NotImplementedException();
	}

	public <T extends Filter> void createFilter(final Class<T> type,
			final Continuation<T> cont) throws IOException {
		// TODO: Implement this part
		throw new NotImplementedException();
	}

	public <T extends Mixer> void createMixer(final Class<T> type,
			final Continuation<T> cont) throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.AsyncClient service = manager
					.getMediaServerServiceAsync();
			Field field = type.getDeclaredField(Mixer.MIXER_ID_FIELD_NAME);
			service.createMixer(
					mediaObject,
					field.getInt(type),
					new AsyncMethodCallback<MediaServerService.AsyncClient.createMixer_call>() {
						@Override
						public void onComplete(createMixer_call response) {
							try {
								com.kurento.kms.api.MediaObject mixer = response
										.getResult();
								Constructor<T> constructor = type
										.getDeclaredConstructor(com.kurento.kms.api.MediaObject.class);
								cont.onSuccess(constructor.newInstance(mixer));
							} catch (MediaServerException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (TException e) {
								cont.onError(new IOException(e.getMessage(), e));
							} catch (NoSuchMethodException e) {
								cont.onError(new IllegalArgumentException());
							} catch (SecurityException e) {
								cont.onError(new IllegalArgumentException());
							} catch (InstantiationException e) {
								cont.onError(new IllegalArgumentException());
							} catch (IllegalAccessException e) {
								cont.onError(new IllegalArgumentException());
							} catch (IllegalArgumentException e) {
								cont.onError(new IllegalArgumentException());
							} catch (InvocationTargetException e) {
								cont.onError(new IllegalArgumentException());
							}
						}

						@Override
						public void onError(Exception exception) {
							cont.onError(exception);
						}
					});
			manager.releaseMediaServerServiceAsync(service);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

}
