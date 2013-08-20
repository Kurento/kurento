package com.kurento.kmf.media;

import java.io.IOException;

import javax.annotation.PreDestroy;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.kurento.kms.api.EndPointType;
import com.kurento.kms.api.MediaObjectId;
import com.kurento.kms.api.MediaObjectType;
import com.kurento.kms.api.MediaObjectTypeUnion;
import com.kurento.kms.api.MediaPadType;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MediaServerService.AsyncClient.createMediaPipeline_call;
import com.kurento.kms.api.MixerType;
import com.kurento.kms.api.SdpEndPointType;
import com.kurento.kms.api.UriEndPointType;

public class MediaPipelineFactory {

	@Autowired
	private MediaServerHandler handler;

	@Autowired
	private MediaServerServiceManager mssm;

	@Autowired
	private ApplicationContext applicationContext;

	MediaPipelineFactory() {
	}

	@PreDestroy
	public void destroy() {
		mssm.destroy();
	}

	/* SYNC */

	public MediaPipeline createMediaPipeline() throws MediaException {
		try {
			MediaServerService.Client service = mssm.getMediaServerService();
			MediaObjectId mediaPipelineId = service.createMediaPipeline(handler
					.getHandlerId());
			mssm.releaseMediaServerService(service);
			return (MediaPipeline) applicationContext.getBean("mediaPipeline",
					mediaPipelineId);
		} catch (MediaServerException e) {
			throw new MediaException(e.getMessage(), e);
		} catch (TException e) {
			throw new MediaException(e.getMessage(), e);
		} catch (IOException e) {
			throw new MediaException(e.getMessage(), e);
		}
	}

	/* ASYNC */

	public void createMediaPipeline(final Continuation<MediaPipeline> cont)
			throws IOException {
		try {
			MediaServerService.AsyncClient service = mssm
					.getMediaServerServiceAsync();
			service.createMediaPipeline(
					handler.getHandlerId(),
					new AsyncMethodCallback<MediaServerService.AsyncClient.createMediaPipeline_call>() {
						@Override
						public void onComplete(createMediaPipeline_call response) {
							try {
								MediaObjectId mediaPipelineId = response
										.getResult();
								cont.onSuccess((MediaPipeline) applicationContext
										.getBean("mediaPipeline",
												mediaPipelineId));
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
			mssm.releaseMediaServerServiceAsync(service);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	protected MediaObject getMediaObject(MediaObjectId mediaObjectId) {
		MediaObjectTypeUnion union = mediaObjectId.getType();

		if (union.isSetMediaObject()) {
			MediaObjectType mediaObjectType = union.getMediaObject();
			if (MediaObjectType.MEDIA_PIPELINE.equals(mediaObjectType)) {
				return (MediaPipeline) applicationContext.getBean(
						"mediaObject", MediaPipeline.class, mediaObjectId);
			}
		} else if (union.isSetMediaPad()) {
			// MediaPad will not be a parent of any media object
			MediaPadType padType = union.getMediaPad();
			if (MediaPadType.MEDIA_SRC.equals(padType)) {
				return (MediaSrc) applicationContext.getBean("mediaObject",
						MediaSrc.class, mediaObjectId);
			} else if (MediaPadType.MEDIA_SINK.equals(padType)) {
				return (MediaSink) applicationContext.getBean("mediaObject",
						MediaSink.class, mediaObjectId);
			}
		} else if (union.isSetEndPoint()) {
			EndPointType endPointType = union.getEndPoint();
			if (EndPointType.HTTP_END_POINT.equals(endPointType)) {
				return (HttpEndPoint) applicationContext.getBean("mediaObject",
						HttpEndPoint.class, mediaObjectId);
			} else if (EndPointType.MIXER_END_POINT.equals(endPointType)) {
				return (MixerEndPoint) applicationContext.getBean(
						"mediaObject", MixerEndPoint.class, mediaObjectId);
			}
		} else if (union.isSetSdpEndPoint()) {
			SdpEndPointType sdpEndPointType = union.getSdpEndPoint();
			if (SdpEndPointType.RTP_END_POINT.equals(sdpEndPointType)) {
				return (RtpEndPoint) applicationContext.getBean("mediaObject",
						RtpEndPoint.class, mediaObjectId);
			} else if (SdpEndPointType.WEBRTC_END_POINT.equals(sdpEndPointType)) {
				return new WebRtcEndPoint(mediaObjectId);
			}
		} else if (union.isSetUriEndPoint()) {
			UriEndPointType uriEndPointType = union.getUriEndPoint();
			if (UriEndPointType.PLAYER_END_POINT.equals(uriEndPointType)) {
				return (PlayerEndPoint) applicationContext.getBean(
						"mediaObject", PlayerEndPoint.class, mediaObjectId);
			} else if (UriEndPointType.RECORDER_END_POINT
					.equals(uriEndPointType)) {
				return (RecorderEndPoint) applicationContext.getBean(
						"mediaObject", RecorderEndPoint.class, mediaObjectId);
			}
		} else if (union.isSetMixerType()) {
			MixerType mixerType = union.getMixerType();
			if (MixerType.MAIN_MIXER.equals(mixerType)) {
				return (MainMixer) applicationContext.getBean("mediaObject",
						MainMixer.class, mediaObjectId);
			}
		} else if (union.isSetFilterType()) {
			// TODO: complete when adding a filter
		}

		return null;
	}
}
