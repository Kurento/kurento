package com.kurento.kmf.media;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.apache.commons.lang.NotImplementedException;
import org.apache.thrift.TException;

import com.kurento.kmf.media.internal.MediaServerServiceManager;
import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MixerType;

public class MediaManager extends MediaObject {

	private static final long serialVersionUID = 1L;

	MediaManager(com.kurento.kms.api.MediaObject mediaManager) {
		super(mediaManager);
	}

	/* SYNC */

	public <T extends SdpEndPoint> T createSdpEndPoint(Class<T> type, String sdp)
			throws MediaException, IOException {
		throw new NotImplementedException();
		// TODO: Implement this method
	}

	public <T extends SdpEndPoint> T createSdpEndPoint(Class<T> type)
			throws MediaException, IOException {
		throw new NotImplementedException();
		// TODO: Implement this method
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

	public <T extends Mixer> T createMixer(Class<T> type)
			throws MediaException, IOException {
		MediaServerService.Client service = MediaServerServiceManager
				.getMediaServerService();

		try {
			Field field;
			try {
				field = type.getDeclaredField(Mixer.MIXER_TYPE_FIELD_NAME);
			} catch (NoSuchFieldException e1) {
				throw new IllegalArgumentException();
			} catch (SecurityException e1) {
				throw new IllegalArgumentException();
			}
			com.kurento.kms.api.MediaObject mixer;
			try {
				mixer = service.createMixer(mediaObject,
						(MixerType) field.get(type));
			} catch (IllegalArgumentException e1) {
				throw new IllegalArgumentException();
			} catch (IllegalAccessException e1) {
				throw new IllegalArgumentException();
			}
			MediaServerServiceManager.releaseMediaServerService(service);
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

	public <T extends Filter> T createFilter(Class<T> type)
			throws MediaException, IOException {
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
		throw new NotImplementedException();
		// TODO: Implement this method
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
		throw new NotImplementedException();
		// TODO: Implement this method
	}

}
