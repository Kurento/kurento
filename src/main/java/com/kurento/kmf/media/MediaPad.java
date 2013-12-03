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

import com.kurento.kms.thrift.api.KmsMediaType;

/**
 * A {@code MediaPad} is an element´s interface with the outside world. Data
 * streams from the {@link MediaSource} pad to another element's
 * {@link MediaSink} pad.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public interface MediaPad extends MediaObject {

	/**
	 * Obtains the {@link MediaElement} that encloses this pad
	 * 
	 * @return The element
	 */
	MediaElement getMediaElement();

	/**
	 * Obtains the {@link MediaElement} that encloses this pad
	 * 
	 * @param cont
	 */
	void getMediaElement(Continuation<MediaElement> cont);

	/**
	 * Obtains the type of media that this pad accepts.
	 * 
	 * @return The type of media.
	 */
	KmsMediaType getMediaType();

	/**
	 * Obtains the description for this pad.
	 * 
	 * @return The description.
	 */
	String getMediaDescription();
}
