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

import com.kurento.kms.thrift.api.KmsMediaType;

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
	Collection<MediaSource> getMediaSrcs(KmsMediaType mediaType);

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
	Collection<MediaSource> getMediaSrcs(KmsMediaType mediaType,
			String description);

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
	Collection<MediaSink> getMediaSinks(KmsMediaType mediaType);

	/**
	 * Returns {@link MediaSink} of a certain type and description, associated
	 * to this element
	 * 
	 * @param mediaType
	 *            The type of media from the sink
	 * @param description
	 * @return A list of sinks. The list will be empty if no sinks are found.
	 */
	Collection<MediaSink> getMediaSinks(KmsMediaType mediaType,
			String description);

	/**
	 * Connects all {@link MediaSource} of this element belonging to the
	 * specified {@link KmsMediaType} to the corresponding {@link MediaSink
	 * } of
	 * the target {@link MediaElement}.
	 * 
	 * This method will throw an exception if any of the following occur: <br>
	 * <ul>
	 * <li>The number of sources for the specified {@link KmsMediaType} in this
	 * element is different than the number of sinks on the target element.
	 * <li>There are duplicate mediaDescriptions on this' element sources for
	 * the specified {@link KmsMediaType}.
	 * <li>There are duplicate mediaDescriptions on target's element sinks for
	 * the specified {@link KmsMediaType}.
	 * <li>Target sinks' media descriptions are different form this sources'
	 * media descriptions for the specified {@link KmsMediaType}
	 * </ul>
	 * 
	 * This method is not transactional. In case of exception some of this
	 * element sources may be connected with target sinks.
	 * 
	 * @param sink
	 *            the target {@MediaElement} from which
	 *            {@link MediaSink} will be obtained
	 * @param mediaType
	 *            the {@KmsMediaType} of the pads that will be
	 *            connected.
	 */
	void connect(MediaElement sink, KmsMediaType mediaType);

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
	void connect(MediaElement sink, KmsMediaType mediaType,
			String mediaDescription);

	/**
	 * Returns all {@link MediaSource} from this element
	 * 
	 * @param cont
	 *            A list of sources. The list will be empty if no sources are
	 *            found.
	 */
	void getMediaSrcs(final Continuation<Collection<MediaSource>> cont);

	/**
	 * Returns {@link MediaSource} of a certain type, associated to this element
	 * 
	 * @param mediaType
	 *            The type of media from the sources
	 * @param cont
	 *            A list of sources. The list will be empty if no sources are
	 *            found.
	 */
	void getMediaSrcs(KmsMediaType mediaType,
			final Continuation<Collection<MediaSource>> cont);

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
	void getMediaSrcs(KmsMediaType mediaType, String description,
			final Continuation<Collection<MediaSource>> cont);

	/**
	 * 
	 * @param cont
	 *            A list of sinks. The list will be empty if no sinks are found.
	 */
	void getMediaSinks(final Continuation<Collection<MediaSink>> cont);

	/**
	 * Returns {@link MediaSink} of a certain type, associated to this element
	 * 
	 * @param mediaType
	 *            The type of media from the sink
	 * @param cont
	 *            A list of sinks. The list will be empty if no sinks are found.
	 */
	void getMediaSinks(KmsMediaType mediaType,
			final Continuation<Collection<MediaSink>> cont);

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
	void getMediaSinks(KmsMediaType mediaType, String description,
			final Continuation<Collection<MediaSink>> cont);

	/**
	 * Connects all {@link MediaSource} of this element belonging to the
	 * specified {@link KmsMediaType} to the corresponding {@link MediaSink
	 * } of
	 * the target {@link MediaElement}.
	 * 
	 * This method will throw an exception if any of the following occur: <br>
	 * <ul>
	 * <li>The number of sources for the specified {@link KmsMediaType} in this
	 * element is different than the number of sinks on the target element.
	 * <li>There are duplicate mediaDescriptions on this' element sources for
	 * the specified {@link KmsMediaType}.
	 * <li>There are duplicate mediaDescriptions on target's element sinks for
	 * the specified {@link KmsMediaType}.
	 * <li>Target sinks' media descriptions are different form this sources'
	 * media descriptions for the specified {@link KmsMediaType}
	 * </ul>
	 * 
	 * This method is not transactional. In case of exception some of this
	 * element sources may be connected with target sinks.
	 * 
	 * @param sink
	 *            the target {@MediaElement} from which
	 *            {@link MediaSink} will be obtained
	 * @param mediaType
	 *            the {@KmsMediaType} of the pads that will be
	 *            connected.
	 * @param cont
	 */
	void connect(MediaElement sink, KmsMediaType mediaType,
			final Continuation<Void> cont);

	/**
	 * TODO: invokes the method above for all available MediaTypes
	 * 
	 * This method is not transactional. In case of exception some of this
	 * element sources may be connected with target sinks.
	 * 
	 * @param sink
	 * @param cont
	 */
	void connect(MediaElement sink, final Continuation<Void> cont);

	/**
	 * TODO: only connects if there is exactly one source and one sink for the
	 * specified media type and description
	 * 
	 * @param sink
	 * @param mediaType
	 * @param mediaDescription
	 * @param cont
	 */
	void connect(MediaElement sink, KmsMediaType mediaType,
			String mediaDescription, final Continuation<Void> cont);

}
