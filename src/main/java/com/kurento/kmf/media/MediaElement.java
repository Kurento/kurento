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

import java.util.Collection;

/**
 * Basic building blocks of the media server, that can be interconnected through
 * the API. A {@code MediaElement} is a module that encapsulates a specific
 * media capability. They can be connected to create media pipelines where those
 * capabilities are applied, in sequence, to the stream going through the
 * pipeline.
 * <p>
 * {@code MediaElement} objects are classified by its supported media type
 * (audio, video, etc.) and the flow direction: {@link MediaSource} pads are
 * intended for media delivery while {@link MediaSink} behave as reception
 * points.
 * </p>
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public interface MediaElement extends MediaObject {

	/**
	 * Returns all {@link MediaSource} from this element
	 * 
	 * @return A list of sources. The list will be empty if no sources are
	 *         found.
	 */
	Collection<MediaSource> getMediaSrcs();

	/**
	 * Returns {@link MediaSource} of a certain type, associated to this element
	 * 
	 * @param mediaType
	 *            The type of media from the sources
	 * @return A list of sources. The list will be empty if no sources are
	 *         found.
	 */
	Collection<MediaSource> getMediaSrcs(MediaType mediaType);

	/**
	 * Returns {@link MediaSource} of a certain type and description, associated
	 * to this element
	 * 
	 * @param mediaType
	 *            The type of media from the sources
	 * @param description
	 * @return A list of sources. The list will be empty if no sources are
	 *         found.
	 */
	Collection<MediaSource> getMediaSrcs(MediaType mediaType, String description);

	/**
	 * @return A list of sinks. The list will be empty if no sinks are found.
	 */
	Collection<MediaSink> getMediaSinks();

	/**
	 * Returns {@link MediaSink} of a certain type, associated to this element
	 * 
	 * @param mediaType
	 *            The type of media from the sink
	 * @return A list of sinks. The list will be empty if no sinks are found.
	 */
	Collection<MediaSink> getMediaSinks(MediaType mediaType);

	/**
	 * Returns {@link MediaSink} of a certain type and description, associated
	 * to this element
	 * 
	 * @param mediaType
	 *            The type of media from the sink
	 * @param description
	 * @return A list of sinks. The list will be empty if no sinks are found.
	 */
	Collection<MediaSink> getMediaSinks(MediaType mediaType, String description);

	/**
	 * Connects all {@link MediaSource} of this element belonging to the
	 * specified {@link MediaType} to the corresponding {@link MediaSink
	 * } of the
	 * target {@link MediaElement}.
	 * 
	 * This method will throw an exception if any of the following occur: <br>
	 * <ul>
	 * <li>The number of sources for the specified {@link MediaType} in this
	 * element is different than the number of sinks on the target element.
	 * <li>There are duplicate mediaDescriptions on this' element sources for
	 * the specified {@link MediaType}.
	 * <li>There are duplicate mediaDescriptions on target's element sinks for
	 * the specified {@link MediaType}.
	 * <li>Target sinks' media descriptions are different form this sources'
	 * media descriptions for the specified {@link MediaType}
	 * </ul>
	 * 
	 * This method is not transactional. In case of exception some of this
	 * element sources may be connected with target sinks.
	 * 
	 * @param sink
	 *            the target {@link MediaElement} from which
	 *            {@link MediaSink} will be obtained
	 * @param mediaType
	 *            the {@link MediaType} of the pads that will be
	 *            connected.
	 */
	void connect(MediaElement sink, MediaType mediaType);

	/**
	 * TODO: invokes the method above for all available MediaTypes
	 * 
	 * This method is not transactional. In case of exception some of this
	 * element sources may be connected with target sinks.
	 * 
	 * @param sink
	 */
	void connect(MediaElement sink);

	/**
	 * TODO: only connects if there is exactly one source and one sink for the
	 * specified media type and description
	 * 
	 * @param sink
	 * @param mediaType
	 * @param mediaDescription
	 */
	void connect(MediaElement sink, MediaType mediaType, String mediaDescription);

	/**
	 * Returns all {@link MediaSource} from this element
	 * 
	 * @param cont
	 *            A list of sources. The list will be empty if no sources are
	 *            found.
	 */
	void getMediaSrcs(Continuation<Collection<MediaSource>> cont);

	/**
	 * Returns {@link MediaSource} of a certain type, associated to this element
	 * 
	 * @param mediaType
	 *            The type of media from the sources
	 * @param cont
	 *            A list of sources. The list will be empty if no sources are
	 *            found.
	 */
	void getMediaSrcs(MediaType mediaType,
			Continuation<Collection<MediaSource>> cont);

	/**
	 * Returns {@link MediaSource} of a certain type and description, associated
	 * to this element
	 * 
	 * @param mediaType
	 *            The type of media from the sources
	 * @param description
	 * @param cont
	 *            A list of sources. The list will be empty if no sources are
	 *            found.
	 */
	void getMediaSrcs(MediaType mediaType, String description,
			Continuation<Collection<MediaSource>> cont);

	/**
	 * 
	 * @param cont
	 *            A list of sinks. The list will be empty if no sinks are found.
	 */
	void getMediaSinks(Continuation<Collection<MediaSink>> cont);

	/**
	 * Returns {@link MediaSink} of a certain type, associated to this element
	 * 
	 * @param mediaType
	 *            The type of media from the sink
	 * @param cont
	 *            A list of sinks. The list will be empty if no sinks are found.
	 */
	void getMediaSinks(MediaType mediaType,
			Continuation<Collection<MediaSink>> cont);

	/**
	 * Returns {@link MediaSink} of a certain type and description, associated
	 * to this element
	 * 
	 * @param mediaType
	 *            The type of media from the sink
	 * @param description
	 * @param cont
	 *            A list of sinks. The list will be empty if no sinks are found.
	 */
	void getMediaSinks(MediaType mediaType, String description,
			Continuation<Collection<MediaSink>> cont);

	/**
	 * Connects all {@link MediaSource} of this element belonging to the
	 * specified {@link MediaType} to the corresponding {@link MediaSink
	 * } of the
	 * target {@link MediaElement}.
	 * 
	 * This method will throw an exception if any of the following occur: <br>
	 * <ul>
	 * <li>The number of sources for the specified {@link MediaType} in this
	 * element is different than the number of sinks on the target element.
	 * <li>There are duplicate mediaDescriptions on this' element sources for
	 * the specified {@link MediaType}.
	 * <li>There are duplicate mediaDescriptions on target's element sinks for
	 * the specified {@link MediaType}.
	 * <li>Target sinks' media descriptions are different form this sources'
	 * media descriptions for the specified {@link MediaType}
	 * </ul>
	 * 
	 * This method is not transactional. In case of exception some of this
	 * element sources may be connected with target sinks.
	 * 
	 * @param sink
	 *            the target {@link MediaElement} from which
	 *            {@link MediaSink} will be obtained
	 * @param mediaType
	 *            the {@link MediaType} of the pads that will be
	 *            connected.
	 * @param cont
	 */
	void connect(MediaElement sink, MediaType mediaType, Continuation<Void> cont);

	/**
	 * TODO: invokes the method above for all available MediaTypes
	 * 
	 * This method is not transactional. In case of exception some of this
	 * element sources may be connected with target sinks.
	 * 
	 * @param sink
	 * @param cont
	 */
	void connect(MediaElement sink, Continuation<Void> cont);

	/**
	 * TODO: only connects if there is exactly one source and one sink for the
	 * specified media type and description
	 * 
	 * @param sink
	 * @param mediaType
	 * @param mediaDescription
	 * @param cont
	 */
	void connect(MediaElement sink, MediaType mediaType,
			String mediaDescription, Continuation<Void> cont);

}
