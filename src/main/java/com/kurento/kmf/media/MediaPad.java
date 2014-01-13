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
	 * Obtains the type of media that this pad accepts.
	 * 
	 * @return The type of media.
	 */
	MediaType getMediaType();

	/**
	 * Obtains the description for this pad.
	 * 
	 * @return The description.
	 */
	String getMediaDescription();

	/**
	 * Obtains the {@link MediaElement} that encloses this pad
	 * 
	 * @param cont
	 *            An asynchronous callback handler. The {@code onSuccess} method
	 *            from the handler will be invoked if the command executed
	 *            correctly, receiving as parameter a the element that encloses
	 *            the pad
	 */
	void getMediaElement(Continuation<MediaElement> cont);

	/**
	 * Obtains the type of media that this pad accepts.
	 * <p>
	 * This method does not make a request to the media server, and is included
	 * to keep the simmetry with the rest of methods from the API.
	 * </p>
	 * 
	 * @param cont
	 *            An asynchronous callback handler. The {@code onSuccess} method
	 *            from the handler will be invoked if the command executed
	 *            correctly, receiving as parameter type of pad
	 */
	void getMediaType(Continuation<MediaType> cont);

	/**
	 * Obtains the description for this pad.
	 * <p>
	 * This method does not make a request to the media server, and is included
	 * to keep the simmetry with the rest of methods from the API.
	 * </p>
	 * 
	 * @param cont
	 *            An asynchronous callback handler. The {@code onSuccess} method
	 *            from the handler will be invoked if the command executed
	 *            correctly, receiving as parameter a string with the
	 *            description
	 */
	void getMediaDescription(Continuation<String> cont);

}
