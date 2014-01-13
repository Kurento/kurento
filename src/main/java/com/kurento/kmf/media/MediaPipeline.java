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

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.FaceOverlayFilter.FaceOverlayFilterBuilder;
import com.kurento.kmf.media.GStreamerFilter.GStreamerFilterBuilder;
import com.kurento.kmf.media.HttpEndpoint.HttpEndpointBuilder;
import com.kurento.kmf.media.JackVaderFilter.JackVaderFilterBuilder;
import com.kurento.kmf.media.PlateDetectorFilter.PlateDetectorFilterBuilder;
import com.kurento.kmf.media.PlayerEndpoint.PlayerEndpointBuilder;
import com.kurento.kmf.media.PointerDetectorFilter.PointerDetectorFilterBuilder;
import com.kurento.kmf.media.RecorderEndpoint.RecorderEndpointBuilder;
import com.kurento.kmf.media.RtpEndpoint.RtpEndpointBuilder;
import com.kurento.kmf.media.WebRtcEndpoint.WebRtcEndpointBuilder;
import com.kurento.kmf.media.ZBarFilter.ZBarFilterBuilder;
import com.kurento.kmf.media.params.MediaParam;

/**
 * A pipeline is a container for a collection of {@link MediaElement} and
 * {@link MediaMixer}. This interface offers the methods needed to control the
 * creation and connection of elements inside a certain pipeline.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public interface MediaPipeline extends MediaObject {

	/**
	 * Connects two {@link MediaElement}
	 * 
	 * @param source
	 *            The source element, whose output will be used as input for the
	 *            sink
	 * @param sink
	 *            The sink
	 */
	void connect(MediaElement source, MediaElement sink);

	/**
	 * Connects two {@link MediaElement}
	 * 
	 * @param source
	 *            The source element, whose output will be used as input for the
	 *            sink
	 * @param sink
	 *            The sink
	 * @param mediaType
	 */
	void connect(MediaElement source, MediaElement sink, MediaType mediaType);

	/**
	 * Connects two {@link MediaElement}
	 * 
	 * @param source
	 *            The source element, whose output will be used as input for the
	 *            sink
	 * @param sink
	 *            The sink
	 * @param mediaType
	 *            The type of media that will flow form source to sink
	 * @param mediaDescription
	 *            A description text
	 */
	void connect(MediaElement source, MediaElement sink, MediaType mediaType,
			String mediaDescription);

	/**
	 * Connects two media {@link MediaElement}
	 * 
	 * @param source
	 *            The source element, whose output will be used as input for the
	 *            sink
	 * @param sink
	 *            The sink
	 * @param cont
	 *            An asynchronous callback handler. The {@code onSuccess} method
	 *            from the handler will be invoked in case of correct
	 *            connection, but no extra information will be provided.
	 */
	void connect(MediaElement source, MediaElement sink, Continuation<Void> cont);

	/**
	 * Connects two media {@link MediaElement}
	 * 
	 * @param source
	 *            The source element, whose output will be used as input for the
	 *            sink
	 * @param sink
	 * @param mediaType
	 *            The type of media that will flow form source to sink
	 * @param cont
	 *            An asynchronous callback handler. The {@code onSuccess} method
	 *            from the handler will be invoked in case of correct
	 *            connection, but no extra information will be provided.
	 */
	void connect(MediaElement source, MediaElement sink, MediaType mediaType,
			Continuation<Void> cont);

	/**
	 * Connects two media {@link MediaElement}
	 * 
	 * @param source
	 *            The source element, whose output will be used as input for the
	 *            sink
	 * @param sink
	 *            The sink
	 * @param mediaType
	 *            The type of media that will flow form source to sink
	 * @param mediaDescription
	 *            A description text
	 * @param cont
	 *            An asynchronous callback handler. The {@code onSuccess} method
	 *            from the handler will be invoked in case of correct
	 *            connection, but no extra information will be provided.
	 */
	void connect(MediaElement source, MediaElement sink, MediaType mediaType,
			String mediaDescription, Continuation<Void> cont);

	/**
	 * Obtains the builder for an {@link HttpEndpoint}.
	 * 
	 * @return The builder
	 */
	HttpEndpointBuilder newHttpEndpoint();

	/**
	 * Obtains the builder for an {@link RtpEndpoint}.
	 * 
	 * @return The builder
	 */
	RtpEndpointBuilder newRtpEndpoint();

	/**
	 * Obtains the builder for an {@link WebRtcEndpoint}.
	 * 
	 * @return The builder
	 */
	WebRtcEndpointBuilder newWebRtcEndpoint();

	/**
	 * Obtains the builder for an {@link PlayerEndpoint}.
	 * 
	 * @param uri
	 *            The URI, according to the grammar specified in <a
	 *            href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
	 * @return The builder
	 * @throws KurentoMediaFrameworkException
	 *             If the given string violates RFC&nbsp;2396
	 */
	PlayerEndpointBuilder newPlayerEndpoint(String uri);

	/**
	 * Obtains the builder for an {@link PlayerEndpoint}.
	 * 
	 * @param uri
	 *            The URI
	 * @return The builder
	 */
	PlayerEndpointBuilder newPlayerEndpoint(URI uri);

	/**
	 * Obtains the builder for an {@link RecorderEndpoint}.
	 * 
	 * @param uri
	 *            The URI, according to the grammar specified in <a
	 *            href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
	 * @return The builder
	 * @throws KurentoMediaFrameworkException
	 *             If the given string violates RFC&nbsp;2396
	 */
	RecorderEndpointBuilder newRecorderEndpoint(String uri);

	/**
	 * Obtains the builder for a {@link RecorderEndpoint}.
	 * 
	 * @param uri
	 *            The URI
	 * @return The builder
	 */
	RecorderEndpointBuilder newRecorderEndpoint(URI uri);

	/**
	 * Obtains the builder for a {@link ZBarFilter}.
	 * 
	 * @return The builder
	 */
	ZBarFilterBuilder newZBarFilter();

	/**
	 * Obtains the builder for a {@link JackVaderFilter}.
	 * 
	 * @return The builder
	 */
	JackVaderFilterBuilder newJackVaderFilter();

	/**
	 * Obtains the builder for a {@link PointerDetectorFilter}.
	 * 
	 * @return The builder
	 */
	PointerDetectorFilterBuilder newPointerDetectorFilter();

	/**
	 * Obtains the builder for a {@link PlateDetectorFilter}.
	 * 
	 * @return The builder
	 */
	PlateDetectorFilterBuilder newPlateDetectorFilter();

	/**
	 * Obtains the builder for a {@link FaceOverlayFilter}.
	 * 
	 * @return The builder
	 */
	FaceOverlayFilterBuilder newFaceOverlayFilter();

	/**
	 * Obtains the builder for a {@link GStreamerFilterBuilder}.
	 * 
	 * @param command
	 *            command that would be used to instantiate the filter, as in
	 *            gst-launch
	 * 
	 * @return The builder
	 */
	GStreamerFilterBuilder newGStreamerFilter(String command);

	/**
	 * Creates a {@link MediaElement} associated to this pipeline.
	 * 
	 * @param elementType
	 *            The name of the element type
	 * @return The builder
	 */
	MediaElement createMediaElement(String elementType);

	/**
	 * Creates a {@link MediaElement} associated to this pipeline.
	 * 
	 * @param elementType
	 *            The name of the element type
	 * @param params
	 *            Parameters to be used by the server to configure the object
	 *            during creation
	 * @return The builder
	 */
	MediaElement createMediaElement(String elementType,
			Map<String, MediaParam> params);

	/**
	 * Creates a {@link MediaMixer} associated to this pipeline.
	 * 
	 * @param mixerType
	 *            The name of the mixer type
	 * @return The builder
	 */
	MediaMixer createMediaMixer(String mixerType);

	/**
	 * Creates a {@link MediaMixer} associated to this pipeline.
	 * 
	 * @param mixerType
	 *            The name of the mixer type
	 * @param params
	 *            Parameters to be used by the server to configure the object
	 *            during creation
	 * @return The builder
	 */
	MediaMixer createMediaMixer(String mixerType, Map<String, MediaParam> params);

	@Override
	MediaPipeline getParent();

	/**
	 * Creates a {@link MediaElement} associated to this pipeline.
	 * 
	 * @param elementType
	 *            The name of the element type
	 * @param cont
	 *            An asynchronous callback handler. If the element was
	 *            successfully created, the {@code onSuccess} method from the
	 *            handler will receive a {@link MediaElement} stub from the
	 *            media server.
	 */
	<T extends MediaElement> void createMediaElement(String elementType,
			Continuation<T> cont);

	/**
	 * Creates a {@link MediaElement} associated to this pipeline.
	 * 
	 * @param elementType
	 *            The name of the element type
	 * @param params
	 *            Parameters to be used by the server to configure the object
	 *            during creation
	 * @param cont
	 *            An asynchronous callback handler. If the element was
	 *            successfully created, the {@code onSuccess} method from the
	 *            handler will receive a {@link MediaElement} stub from the
	 *            media server.
	 */
	<T extends MediaElement> void createMediaElement(String elementType,
			Map<String, MediaParam> params, Continuation<T> cont);

	/**
	 * Creates a {@link MediaMixer} associated to this pipeline.
	 * 
	 * @param mixerType
	 *            The name of the mixer type
	 * @param cont
	 *            An asynchronous callback handler. If the mixer was
	 *            successfully created, the {@code onSuccess} method from the
	 *            handler will receive a {@link MediaMixer} stub from the media
	 *            server.
	 */
	<T extends MediaMixer> void createMediaMixer(String mixerType,
			Continuation<T> cont);

	/**
	 * Creates a {@link MediaMixer} associated to this pipeline.
	 * 
	 * @param mixerType
	 *            The name of the mixer type
	 * @param params
	 *            Parameters to be used by the server to configure the object
	 *            during creation
	 * @param cont
	 *            An asynchronous callback handler. If the mixer was
	 *            successfully created, the {@code onSuccess} method from the
	 *            handler will receive a {@link MediaMixer} stub from the media
	 *            server.
	 */
	<T extends MediaMixer> void createMediaMixer(String mixerType,
			Map<String, MediaParam> params, Continuation<T> cont);

}
