package com.kurento.kmf.media;

import java.io.IOException;
import java.io.Serializable;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.transport.TTransportException;

import com.kurento.kmf.media.internal.MediaServerServiceManager;
import com.kurento.kms.api.EndPointType;
import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaObjectType;
import com.kurento.kms.api.MediaObjectTypeUnion;
import com.kurento.kms.api.MediaPadType;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MediaServerService.AsyncClient.getParent_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.release_call;
import com.kurento.kms.api.MixerType;
import com.kurento.kms.api.NoParentException;
import com.kurento.kms.api.SdpEndPointType;
import com.kurento.kms.api.UriEndPointType;

public abstract class MediaObject implements Serializable {

	private static final long serialVersionUID = 1L;

	final com.kurento.kms.api.MediaObject mediaObject;

	MediaObject(com.kurento.kms.api.MediaObject mediaObject) {
		this.mediaObject = mediaObject;
	}

	/* SYNC */

	public MediaObject getParent() throws IOException {
		MediaServerServiceManager manager = MediaServerServiceManager
				.getInstance();
		MediaServerService.Client service;
		try {
			service = manager.getMediaServerService();
		} catch (TTransportException e) {
			throw new IOException(e.getMessage(), e);
		}

		try {
			return getParent(service.getParent(mediaObject));
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (NoParentException e) {
			return null;
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			manager.releaseMediaServerService(service);
		}
	}

	public void release() throws IOException {
		MediaServerServiceManager manager = MediaServerServiceManager
				.getInstance();
		MediaServerService.Client service;
		try {
			service = manager.getMediaServerService();
		} catch (TTransportException e) {
			throw new IOException(e.getMessage(), e);
		}

		try {
			service.release(mediaObject);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			manager.releaseMediaServerService(service);
		}
	}

	/* ASYNC */

	public void getParent(final Continuation<MediaObject> cont)
			throws IOException {
		MediaServerServiceManager manager = MediaServerServiceManager
				.getInstance();
		MediaServerService.AsyncClient service;
		try {
			service = manager.getMediaServerServiceAsync();
		} catch (TTransportException e) {
			throw new IOException(e.getMessage(), e);
		}

		try {
			service.getParent(
					mediaObject,
					new AsyncMethodCallback<MediaServerService.AsyncClient.getParent_call>() {
						@Override
						public void onComplete(getParent_call response) {
							try {
								MediaObject parent = getParent(response
										.getResult());
								cont.onSuccess(parent);
							} catch (MediaObjectNotFoundException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (MediaServerException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (NoParentException e) {
								cont.onSuccess(null);
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
			manager.releaseMediaServerServiceAsync(service);
		}
	}

	public void release(final Continuation<Void> cont) throws IOException {
		MediaServerServiceManager manager = MediaServerServiceManager
				.getInstance();
		MediaServerService.AsyncClient service;
		try {
			service = manager.getMediaServerServiceAsync();
		} catch (TTransportException e) {
			throw new IOException(e.getMessage(), e);
		}

		try {
			service.release(
					mediaObject,
					new AsyncMethodCallback<MediaServerService.AsyncClient.release_call>() {
						@Override
						public void onComplete(release_call response) {
							try {
								response.getResult();
								cont.onSuccess(null);
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
			manager.releaseMediaServerServiceAsync(service);
		}
	}

	private MediaObject getParent(com.kurento.kms.api.MediaObject parent) {
		MediaObjectTypeUnion union = parent.getType();

		if (union.isSetMediaObject()) {
			MediaObjectType mediaObjectType = union.getMediaObject();
			if (MediaObjectType.MEDIA_MANAGER.equals(mediaObjectType)) {
				return new MediaManager(parent);
			}
		} else if (union.isSetMediaPad()) {
			// MediaPad will not be a parent of any media object
			MediaPadType padType = union.getMediaPad();
			if (MediaPadType.MEDIA_SRC.equals(padType)) {
				return new MediaSrc(parent);
			} else if (MediaPadType.MEDIA_SINK.equals(padType)) {
				return new MediaSink(parent);
			}
		} else if (union.isSetEndPoint()) {
			EndPointType endPointType = union.getEndPoint();
			if (EndPointType.HTTP_END_POINT.equals(endPointType)) {
				return new HttpEndPoint(parent);
			} else if (EndPointType.MIXER_END_POINT.equals(endPointType)) {
				return new MixerEndPoint(parent);
			}
		} else if (union.isSetSdpEndPoint()) {
			SdpEndPointType sdpEndPointType = union.getSdpEndPoint();
			if (SdpEndPointType.RTP_END_POINT.equals(sdpEndPointType)) {
				return new RtpEndPoint(parent);
			} else if (SdpEndPointType.WEBRTC_END_POINT.equals(sdpEndPointType)) {
				return new WebRtcEndPoint(parent);
			}
		} else if (union.isSetUriEndPoint()) {
			UriEndPointType uriEndPointType = union.getUriEndPoint();
			if (UriEndPointType.PLAYER_END_POINT.equals(uriEndPointType)) {
				return new PlayerEndPoint(parent);
			} else if (UriEndPointType.RECORDER_END_POINT
					.equals(uriEndPointType)) {
				return new RecorderEndPoint(parent);
			}
		} else if (union.isSetMixerType()) {
			MixerType mixerType = union.getMixerType();
			if (MixerType.MAIN_MIXER.equals(mixerType)) {
				return new MainMixer(parent);
			}
		} else if (union.isSetFilterType()) {
			// TODO: complete when adding a filter
		}

		return null;
	}

}
