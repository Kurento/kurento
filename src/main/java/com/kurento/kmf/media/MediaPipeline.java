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

import com.kurento.kmf.media.commands.MediaParams;

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

	MediaElement createMediaElement(String elementType, MediaParams params);

	MediaMixer createMediaMixer(String mixerType);

	MediaMixer createMediaMixer(String mixerType, MediaParams params);

	@Override
	MediaPipeline getParent();

	<T extends MediaElement> void createMediaElement(String elementType,
			Continuation<T> cont);

	<T extends MediaElement> void createMediaElement(String elementType,
			MediaParams params, Continuation<T> cont);

	<T extends MediaMixer> void createMediaMixer(String mixerType,
			Continuation<T> cont);

	<T extends MediaMixer> void createMediaMixer(String mixerType,
			MediaParams params, Continuation<T> cont);

}
