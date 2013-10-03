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

public interface MediaSink extends MediaPad {
	/**
	 * Disconnects the current sink from the referred {@link MediaSource}
	 * 
	 * @param src
	 *            The source to disconnect
	 */
	void disconnect(MediaSource src);

	/**
	 * Gets the {@link MediaSource} that is connected to this sink.
	 * 
	 * @return The source connected to this sink.
	 */
	MediaSource getConnectedSrc();

	/**
	 * Connects the current source with a {@link MediaSink}
	 * 
	 * @param sink
	 *            The sink to connect this source
	 * @param cont
	 */
	void disconnect(MediaSink sink, final Continuation<Void> cont);

	/**
	 * Gets all {@link MediaSink} to which this source is connected.
	 * 
	 * @param cont
	 */
	public void getConnectedSrc(final Continuation<MediaSource> cont);
}
