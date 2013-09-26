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
package com.kurento.kmf.content;

import com.kurento.kmf.content.jsonrpc.Constraints;
import com.kurento.kmf.media.MediaElement;

/**
 * TODO: write javadoc
 * 
 * @author llopez
 * 
 */
public interface SdpContentSession extends ContentSession {
	/**
	 * Get the operations related with the video stream (SEND, RECV, ...).
	 * 
	 * @return video stream operation
	 */
	public Constraints getVideoConstraints();

	/**
	 * Get the operations related with the audio stream (SEND, RECV, ...).
	 * 
	 * @return audio stream operation
	 */
	public Constraints getAudioConstraints();

	/**
	 * Start the RTP session.
	 * 
	 * @param sinkElement
	 *            In-going media element
	 * @param sourceElement
	 *            Out-going media element
	 * @throws ContentException
	 *             Exception in the RTP process
	 */
	void start(MediaElement sourceElement, MediaElement... sinkElements);

	// TODO: javadoc
	void start(MediaElement sourceElement, MediaElement sinkElement);
}
