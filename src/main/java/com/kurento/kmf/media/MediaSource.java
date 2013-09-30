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

public interface MediaSource extends MediaPad {
	/**
	 * Connects the current source with a {@link MediaSink}
	 * 
	 * @param sink
	 *            The sink to connect this source
	 */
	public void connect(MediaSink sink);

	/**
	 * Gets all {@link MediaSink} to which this source is connected.
	 * 
	 * @return The list of sinks that the source is connected to.
	 */
	public Collection<MediaSink> getConnectedSinks();

	/**
	 * Connects the current source with a {@link MediaSink}
	 * 
	 * @param sink
	 *            The sink to connect this source
	 */
	public void connect(MediaSink sink, final Continuation<Void> cont);

	/**
	 * Gets all {@link MediaSink} to which this source is connected.
	 * 
	 * @return The list of sinks that the source is connected to.
	 */
	public void getConnectedSinks(final Continuation<Collection<MediaSink>> cont);
}
