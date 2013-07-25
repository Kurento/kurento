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
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			com.kurento.kms.api.MediaObject mixerPort = service
					.createMixerEndPoint(mediaObject);
			manager.releaseMediaServerService(service);
			return new MixerEndPoint(mixerPort);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	/* ASYNC */

	public void createMixerEndPoint(final Continuation<MixerEndPoint> cont)
			throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.AsyncClient service = manager
					.getMediaServerServiceAsync();
			service.createMixerEndPoint(
					mediaObject,
					new AsyncMethodCallback<MediaServerService.AsyncClient.createMixerEndPoint_call>() {
						@Override
						public void onComplete(createMixerEndPoint_call response) {
							try {
								com.kurento.kms.api.MediaObject mixerPort = response
										.getResult();
								cont.onSuccess(new MixerEndPoint(mixerPort));
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
