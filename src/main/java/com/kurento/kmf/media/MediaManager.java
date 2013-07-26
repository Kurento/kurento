package com.kurento.kmf.media;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang.NotImplementedException;
import org.apache.thrift.TException;

import com.kurento.kmf.media.internal.MediaServerServiceManager;
import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
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
					.createUriEndpoint(mediaObject, t, uri);
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
					.createHttpEndpoint(mediaObject);
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
