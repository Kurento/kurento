package com.kurento.kmf.media;

import java.io.IOException;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kmf.media.internal.MediaServerServiceManager;
import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MediaServerService.AsyncClient.createMixerEndPoint_call;

public abstract class Mixer extends MediaObject {

	private static final long serialVersionUID = 1L;

	static final String MIXER_TYPE_FIELD_NAME = "mixerType";

	Mixer(com.kurento.kms.api.MediaObject mixer) {
		super(mixer);
	}

	/* SYNC */

	public MixerEndPoint createMixerEndPoint() throws IOException {
		MediaServerService.Client service = MediaServerServiceManager
				.getMediaServerService();

		try {
			return new MixerEndPoint(service.createMixerEndPoint(mediaObject));
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

	public void createMixerEndPoint(final Continuation<MixerEndPoint> cont)
			throws IOException {
		MediaServerService.AsyncClient service = MediaServerServiceManager
				.getMediaServerServiceAsync();

		try {
			service.createMixerEndPoint(
					mediaObject,
					new AsyncMethodCallback<MediaServerService.AsyncClient.createMixerEndPoint_call>() {
						@Override
						public void onComplete(createMixerEndPoint_call response) {
							try {
								cont.onSuccess(new MixerEndPoint(response
										.getResult()));
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
