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

import com.kurento.kmf.media.HttpEndPoint.HttpEndPointBuilder;
import com.kurento.kmf.media.JackVaderFilter.JackVaderFilterBuilder;
import com.kurento.kmf.media.PlayerEndPoint.PlayerEndPointBuilder;
import com.kurento.kmf.media.RecorderEndPoint.RecorderEndPointBuilder;
import com.kurento.kmf.media.RtpEndPoint.RtpEndPointBuilder;
import com.kurento.kmf.media.WebRtcEndPoint.WebRtcEndPointBuilder;
import com.kurento.kmf.media.ZBarFilter.ZBarFilterBuilder;
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
	HttpEndPointBuilder newHttpEndPoint();

	RtpEndPointBuilder newRtpEndPoint();

	WebRtcEndPointBuilder newWebRtcEndPoint();

	PlayerEndPointBuilder newPlayerEndPoint(String uri);

	PlayerEndPointBuilder newPlayerEndPoint(URI uri);

	RecorderEndPointBuilder newRecorderEndPoint(String uri);

	RecorderEndPointBuilder newRecorderEndPoint(URI uri);

	ZBarFilterBuilder newZBarFilter();

	JackVaderFilterBuilder newJackVaderFilter();

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
