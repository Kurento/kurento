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
 * Special type of pad, used by a media element to generate a media stream.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public interface MediaSource extends MediaPad {
	/**
	 * Connects the current source with a {@link MediaSink}
	 * 
	 * @param sink
	 *            The sink to connect this source
	 */
	void connect(MediaSink sink);

	/**
	 * Gets all {@link MediaSink} to which this source is connected.
	 * 
	 * @return The list of sinks that the source is connected to.
	 */
	Collection<MediaSink> getConnectedSinks();

	/**
	 * Connects the current source with a {@link MediaSink}
	 * 
	 * @param sink
	 *            The sink to connect this source
	 * @param cont
	 *            An asynchronous callback handler. The {@code onSuccess} method
	 *            from the handler will be invoked in case of correct
	 *            connection, but no extra information will be provided.
	 */
	void connect(MediaSink sink, Continuation<Void> cont);

	/**
	 * Gets all {@link MediaSink} to which this source is connected.
	 * 
	 * @param cont
	 *            The list of sinks that the source is connected to.
	 * 
	 */
	void getConnectedSinks(Continuation<Collection<MediaSink>> cont);
}
