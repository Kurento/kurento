package com.kurento.kmf.media;

import com.kurento.kmf.media.commands.MediaParam;
import com.kurento.kms.thrift.api.MediaType;

public interface MediaPipeline extends MediaObject {

	// Creation of specific framework types
	HttpEndPoint createHttpEndPoint();

	RtpEndPoint createRtpEndPoint();

	WebRtcEndPoint createWebRtcEndPoint();

	PlayerEndPoint createPlayerEndPoint(String uri);

	RecorderEndPoint createRecorderEndPoint(String uri);

	ZBarFilter createZBarFilter();

	JackVaderFilter createJackVaderFilter();

	void createHttpEndPoint(Continuation<HttpEndPoint> cont);

	void createRtpEndPoint(Continuation<RtpEndPoint> cont);

	void createWebRtcEndPoint(Continuation<WebRtcEndPoint> cont);

	void createPlayerEndPoint(String uri, Continuation<PlayerEndPoint> cont);

	void createRecorderEndPoint(String uri, Continuation<RecorderEndPoint> cont);

	void createZBarFilter(Continuation<ZBarFilter> cont);

	void createJackVaderFilter(Continuation<JackVaderFilter> cont);

	// Generic creation methods
	MediaElement createMediaElement(String elementType);

	MediaElement createMediaElement(String elementType, MediaParam params);

	MediaMixer createMediaMixer(String mixerType);

	MediaMixer createMediaMixer(String mixerType, MediaParam params);

	@Override
	MediaPipeline getParent();

	<T extends MediaElement> void createMediaElement(String elementType,
			Continuation<T> cont);

	<T extends MediaElement> void createMediaElement(String elementType,
			MediaParam params, Continuation<T> cont);

	<T extends MediaMixer> void createMediaMixer(String mixerType,
			Continuation<T> cont);

	<T extends MediaMixer> void createMediaMixer(String mixerType,
			MediaParam params, Continuation<T> cont);

}
