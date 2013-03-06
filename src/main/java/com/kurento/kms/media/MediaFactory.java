package com.kurento.kms.media;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MediaServerService.AsyncClient.createMediaFactory_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.createMediaPlayer_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.createMediaRecorder_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.createMixer_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.createStream_call;
import com.kurento.kms.media.internal.KmsConstants;
import com.kurento.kms.media.internal.MediaServerServiceManager;

public class MediaFactory extends MediaObject {

	private static final long serialVersionUID = 1L;

	private static String mediaServerAddress;
	private static int mediaServerPort;

	private MediaFactory(com.kurento.kms.api.MediaObject mediaFactory) {
		super(mediaFactory);
	}

	public static void init(Properties properties) throws MediaException {
		processProperties(properties);

		try {
			// TODO: Make this clusterizable to can use some media servers.
			MediaServerServiceManager.init(mediaServerAddress, mediaServerPort);
		} catch (TException e) {
			throw new MediaException(e.getMessage(), e);
		}
	}

	public static void getMediaFactory(final Continuation<MediaFactory> cont)
			throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.AsyncClient service = manager
					.getMediaServerServiceAsync();
			service.createMediaFactory(new AsyncMethodCallback<MediaServerService.AsyncClient.createMediaFactory_call>() {
				@Override
				public void onComplete(createMediaFactory_call response) {
					try {
						com.kurento.kms.api.MediaObject mediaFactory = response
								.getResult();
						cont.onSuccess(new MediaFactory(mediaFactory));
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
			manager.releaseMediaServerServiceAsync(service);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public void getMediaPlayer(String uri, final Continuation<MediaPlayer> cont)
			throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.AsyncClient service = manager
					.getMediaServerServiceAsync();
			service.createMediaPlayer(
					mediaObject,
					new AsyncMethodCallback<MediaServerService.AsyncClient.createMediaPlayer_call>() {
						@Override
						public void onComplete(createMediaPlayer_call response) {
							try {
								com.kurento.kms.api.MediaObject mediaPlayer = response
										.getResult();
								cont.onSuccess(new MediaPlayer(mediaPlayer));
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

	public void getMediaRecorder(String uri,
			final Continuation<MediaRecorder> cont) throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.AsyncClient service = manager
					.getMediaServerServiceAsync();
			service.createMediaRecorder(
					mediaObject,
					new AsyncMethodCallback<MediaServerService.AsyncClient.createMediaRecorder_call>() {
						@Override
						public void onComplete(createMediaRecorder_call response) {
							try {
								com.kurento.kms.api.MediaObject mediaRecorder = response
										.getResult();
								cont.onSuccess(new MediaRecorder(mediaRecorder));
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

	public void getStream(final Continuation<Stream> cont) throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.AsyncClient service = manager
					.getMediaServerServiceAsync();
			service.createStream(
					mediaObject,
					new AsyncMethodCallback<MediaServerService.AsyncClient.createStream_call>() {
						@Override
						public void onComplete(createStream_call response) {
							try {
								com.kurento.kms.api.MediaObject stream = response
										.getResult();
								cont.onSuccess(new Stream(stream));
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

	public <T extends Mixer> void getMixer(final Class<T> clazz,
			final Continuation<T> cont) throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.AsyncClient service = manager
					.getMediaServerServiceAsync();
			Field field = clazz.getDeclaredField(Mixer.MIXER_ID_FIELD_NAME);
			service.createMixer(mediaObject,
					field.getInt(clazz),
					new AsyncMethodCallback<MediaServerService.AsyncClient.createMixer_call>() {
						@Override
						public void onComplete(createMixer_call response) {
							try {
								com.kurento.kms.api.MediaObject mixer = response
										.getResult();
								Constructor<T> constructor = clazz
										.getDeclaredConstructor(com.kurento.kms.api.MediaObject.class);
								cont.onSuccess(constructor.newInstance(mixer));
							} catch (MediaServerException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (TException e) {
								cont.onError(new IOException(e.getMessage(), e));
							} catch (NoSuchMethodException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (SecurityException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (IllegalAccessException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (IllegalArgumentException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (InvocationTargetException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (InstantiationException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
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

	private static void processProperties(Properties properties) {
		if (properties.getProperty(KmsConstants.SERVER_ADDRESS) == null) {
			mediaServerAddress = KmsConstants.DEFAULT_SERVER_ADDRESS;
		} else {
			mediaServerAddress = properties
					.getProperty(KmsConstants.SERVER_ADDRESS);
		}

		if (properties.getProperty(KmsConstants.SERVER_PORT) == null) {
			mediaServerPort = KmsConstants.DEFAULT_SERVER_PORT;
		} else {
			try {
				mediaServerPort = Integer.parseInt(properties
						.getProperty(KmsConstants.SERVER_PORT));
			} catch (NumberFormatException e) {
				mediaServerPort = KmsConstants.DEFAULT_SERVER_PORT;
			}
		}
	}

}
