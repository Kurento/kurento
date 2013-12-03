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
 * Special type of pad, used by a media element to receive a media stream.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
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
	 * Disconnects the current sink from the referred {@link MediaSource}
	 * 
	 * @param sink
	 *            The source to disconnect
	 * @param cont
	 *            An asynchronous callback handler. The {@code onSuccess} method
	 *            from the handler will be invoked in case of correct
	 *            disconnection, but no extra information will be provided.
	 */
	void disconnect(MediaSink sink, Continuation<Void> cont);

	/**
	 * Gets the {@link MediaSource} to which this sink is connected.
	 * 
	 * @param cont
	 *            An asynchronous callback handler. The {@code onSuccess} method
	 *            from the handler will be invoked if the command executed
	 *            correctly, receiving the {@link MediaSource}
	 */
	void getConnectedSrc(Continuation<MediaSource> cont);

}
