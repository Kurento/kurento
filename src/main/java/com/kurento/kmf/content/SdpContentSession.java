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
import com.kurento.kmf.repository.RepositoryItem;

/**
 * Media Session where the client and server use SDP negotiation
 * 
 * A Sdp ContentSession encapsulates a session where a client requests Content
 * using the SDP protocol}
 * 
 * @author Luis López (llopez@gsyc.es)
 * @see <a href="http://en.wikipedia.org/wiki/Session_Description_Protocol">SDP
 *      Protocol</a>
 * 
 */
public interface SdpContentSession extends ContentSession {
	/**
	 * Get the operations related with the video stream (SEND, RECV, ...).
	 * 
	 * @return video stream operation
	 */
	Constraints getVideoConstraints();

	/**
	 * Get the operations related with the audio stream (SEND, RECV, ...).
	 * 
	 * @return audio stream operation
	 */
	Constraints getAudioConstraints();

	/**
	 * TODO
	 * 
	 * @param sourceRepositoryItem
	 *            RepositoryItem generating content into the pipeline
	 * @param sinkRepositoryItem
	 *            RepositoryItem for the pipeline content output
	 * @throws ContentException
	 *             Exception in the RTP process
	 */
	void start(RepositoryItem sourceRepositoryItem,
			RepositoryItem sinkRepositoryItem);

	/**
	 * TODO
	 * 
	 * @param sourceContentPath
	 *            path to the RepositoryItem generating content into the
	 *            pipeline
	 * @param sinkContentPath
	 *            path to the output RepositoryItem for the pipeline content
	 * @throws ContentException
	 *             Exception in the RTP process
	 */
	void start(String sourceContentPath, String sinkContentPath);
}
