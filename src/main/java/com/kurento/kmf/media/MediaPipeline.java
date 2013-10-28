/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.kmf.media;

import java.net.URI;
import java.util.Map;

import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kms.thrift.api.KmsMediaType;

public interface MediaPipeline extends MediaObject {

	/**
	 * Connects two {@link MediaElement}
	 * 
	 * @param source
	 * @param sink
	 */
	void connect(MediaElement source, MediaElement sink);

	/**
	 * Connects two {@link MediaElement}
	 * 
	 * @param source
	 * @param sink
	 * @param mediaType
	 */
	void connect(MediaElement source, MediaElement sink, KmsMediaType mediaType);

	/**
	 * Connects two {@link MediaElement}
	 * 
	 * @param source
	 * @param sink
	 * @param mediaType
	 * @param mediaDescription
	 */
	void connect(MediaElement source, MediaElement sink,
			KmsMediaType mediaType, String mediaDescription);

	/**
	 * Connects two media elements
	 * 
	 * @param source
	 * @param sink
	 * @param cont
	 */
	void connect(MediaElement source, MediaElement sink, Continuation<Void> cont);

	/**
	 * Connects two media elements
	 * 
	 * @param source
	 * @param sink
	 * @param mediaType
	 * @param cont
	 */
	void connect(MediaElement source, MediaElement sink,
			KmsMediaType mediaType, Continuation<Void> cont);

	/**
	 * Connects two media elements
	 * 
	 * @param source
	 * @param sink
	 * @param mediaType
	 * @param mediaDescription
	 * @param meContinuation
	 * @param cont
	 */
	void connect(MediaElement source, MediaElement sink,
			KmsMediaType mediaType, String mediaDescription,
			Continuation<Void> cont);

	// Creation of specific framework types

	/* HTTP ENDPOINT */
	HttpEndPoint createHttpEndPoint();

	HttpEndPoint createHttpEndPoint(int garbagePeriod);

	HttpEndPoint createHttpEndPoint(int cookieLifetime, int disconnectionTimeout);

	HttpEndPoint createHttpEndPoint(int cookieLifetime,
			int disconnectionTimeout, int garbagePeriod);

	void createHttpEndPoint(Continuation<HttpEndPoint> cont);

	void createHttpEndPoint(int garbagePeriod, Continuation<HttpEndPoint> cont);

	void createHttpEndPoint(int cookieLifetime, int disconnectionTimeout,
			Continuation<HttpEndPoint> cont);

	void createHttpEndPoint(int cookieLifetime, int disconnectionTimeout,
			int garbagePeriod, Continuation<HttpEndPoint> cont);

	/* RTP ENDPOINT */

	RtpEndPoint createRtpEndPoint();

	RtpEndPoint createRtpEndPoint(int garbagePeriod);

	void createRtpEndPoint(Continuation<RtpEndPoint> cont);

	void createRtpEndPoint(int garbagePeriod, Continuation<RtpEndPoint> cont);

	/* WEB RTC ENDPOINT */

	WebRtcEndPoint createWebRtcEndPoint();

	WebRtcEndPoint createWebRtcEndPoint(int garbagePeriod);

	void createWebRtcEndPoint(Continuation<WebRtcEndPoint> cont);

	void createWebRtcEndPoint(int garbagePeriod,
			Continuation<WebRtcEndPoint> cont);

	/* PLAYER ENDPOINT */

	PlayerEndPoint createPlayerEndPoint(String uri);

	PlayerEndPoint createPlayerEndPoint(URI uri);

	PlayerEndPoint createPlayerEndPoint(String uri, int garbagePeriod);

	PlayerEndPoint createPlayerEndPoint(URI uri, int garbagePeriod);

	void createPlayerEndPoint(String uri, Continuation<PlayerEndPoint> cont);

	void createPlayerEndPoint(URI uri, Continuation<PlayerEndPoint> cont);

	void createPlayerEndPoint(String uri, int garbagePeriod,
			Continuation<PlayerEndPoint> cont);

	void createPlayerEndPoint(URI uri, int garbagePeriod,
			Continuation<PlayerEndPoint> cont);

	/* RECORDER ENDPOINT */

	RecorderEndPoint createRecorderEndPoint(String uri);

	RecorderEndPoint createRecorderEndPoint(URI uri);

	RecorderEndPoint createRecorderEndPoint(String uri, int garbagePeriod);

	RecorderEndPoint createRecorderEndPoint(URI uri, int garbagePeriod);

	void createRecorderEndPoint(String uri, Continuation<RecorderEndPoint> cont);

	void createRecorderEndPoint(URI uri, Continuation<RecorderEndPoint> cont);

	void createRecorderEndPoint(URI uri, int garbagePeriod,
			Continuation<RecorderEndPoint> cont);

	void createRecorderEndPoint(String uri, int garbagePeriod,
			Continuation<RecorderEndPoint> cont);

	/* ZBAR FILTER */

	ZBarFilter createZBarFilter();

	ZBarFilter createZBarFilter(int garbagePeriod);

	void createZBarFilter(Continuation<ZBarFilter> cont);

	void createZBarFilter(int garbagePeriod, Continuation<ZBarFilter> cont);

	/* JACKVADER FILTER */

	JackVaderFilter createJackVaderFilter();

	JackVaderFilter createJackVaderFilter(int garbagePeriod);

	void createJackVaderFilter(Continuation<JackVaderFilter> cont);

	void createJackVaderFilter(int garbagePeriod,
			Continuation<JackVaderFilter> cont);

	// Generic creation methods
	MediaElement createMediaElement(String elementType);

	MediaElement createMediaElement(String elementType,
			Map<String, MediaParam> params);

	MediaMixer createMediaMixer(String mixerType);

	MediaMixer createMediaMixer(String mixerType, Map<String, MediaParam> params);

	@Override
	MediaPipeline getParent();

	<T extends MediaElement> void createMediaElement(String elementType,
			Continuation<T> cont);

	<T extends MediaElement> void createMediaElement(String elementType,
			Map<String, MediaParam> params, Continuation<T> cont);

	<T extends MediaMixer> void createMediaMixer(String mixerType,
			Continuation<T> cont);

	<T extends MediaMixer> void createMediaMixer(String mixerType,
			Map<String, MediaParam> params, Continuation<T> cont);

}
