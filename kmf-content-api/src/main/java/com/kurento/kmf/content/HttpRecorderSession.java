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

import com.kurento.kmf.media.HttpPostEndpoint;

/**
 * TODO: review & improve javadoc
 * 
 * Defines the operations performed by the RecordRequest object, which is in
 * charge of the recording a content in a Media Server.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Miguel París (mparisdiaz@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
public interface HttpRecorderSession extends HttpContentSession {
	/**
	 * Starts the content exchange on a given media element. TODO: Explain what
	 * playing a media element means TODO: Explain what starts mean
	 * 
	 * @param endpoint
	 *            pluggable media component
	 * @throws ContentException
	 *             Exception in the play
	 */
	void start(HttpPostEndpoint endpoint);
}
