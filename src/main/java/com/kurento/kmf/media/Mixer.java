package com.kurento.kmf.media;

import java.io.IOException;
import java.lang.reflect.Field;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.kurento.kms.api.MediaObjectId;
import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MediaServerService.AsyncClient.createMixerEndPoint_call;
import com.kurento.kms.api.MixerType;

public abstract class Mixer extends MediaObject {

	private static final long serialVersionUID = 1L;

	private static final String MIXER_TYPE_FIELD_NAME = "mixerType";

	@Autowired
	private MediaServerServiceManager mssm;

	@Autowired
	private ApplicationContext applicationContext;

	Mixer(MediaObjectId mixerId) {
		super(mixerId);
	}

	static <T extends Mixer> MixerType getType(Class<T> type) {
		try {
			Field field = type.getDeclaredField(MIXER_TYPE_FIELD_NAME);
			return (MixerType) field.get(type);
		} catch (NoSuchFieldException e) {
			throw new IllegalArgumentException(e);
		} catch (SecurityException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/* SYNC */

	public MixerEndPoint createMixerEndPoint() throws IOException {
		MediaServerService.Client service = mssm.getMediaServerService();

		try {
			return (MixerEndPoint) applicationContext.getBean("mediaObject",
					MixerEndPoint.class,
					service.createMixerEndPoint(mediaObjectId));
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

	public void createMixerEndPoint(final Continuation<MixerEndPoint> cont)
			throws IOException {
		MediaServerService.AsyncClient service = mssm
				.getMediaServerServiceAsync();

		try {
			service.createMixerEndPoint(
					mediaObjectId,
					new AsyncMethodCallback<MediaServerService.AsyncClient.createMixerEndPoint_call>() {
						@Override
						public void onComplete(createMixerEndPoint_call response) {
							try {
								cont.onSuccess((MixerEndPoint) applicationContext
										.getBean("mediaObject",
												MixerEndPoint.class,
												response.getResult()));
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
