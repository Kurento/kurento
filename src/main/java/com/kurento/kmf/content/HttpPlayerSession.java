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

import com.kurento.kmf.media.HttpEndPoint;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.repository.RepositoryItem;

/**
 * Defines the operations performed by the PlayRequest object, which is in
 * charge of the requesting to a content to be retrieved from a Media Server.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Miguel París (mparisdiaz@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
public interface HttpPlayerSession extends ContentSession {

	/**
	 * Starts the content exchange for a given content path. TODO: What is the
	 * content path? TODO: Explain what starts mean
	 * 
	 * TODO: IN ALL STARTS OF ALL CONTENTSESSIONS. Explain that if starts throws
	 * and exception, then the session is invalidated. If you don't manage this
	 * exception, it will end-up in onUnmanagedException method of the handler,
	 * but the session will be terminated there.
	 * 
	 * @param contentPath
	 *            Identifies the content in a meaningful way for the Media
	 *            Server
	 * @throws ContentException
	 *             Exception in the strat
	 */
	void start(String contentPath);

	/**
	 * Starts the content exchange for a given media element. TODO: Explain what
	 * playing a media element means TODO: Explain what starts mean
	 * 
	 * @param source
	 *            pluggable media component
	 * @throws ContentException
	 *             Exception in the play
	 */
	void start(MediaElement source);

	/**
	 * TODO
	 * 
	 * @param repositoryItem
	 */
	void start(RepositoryItem repositoryItem);

	/**
	 * TODO
	 * 
	 * @return
	 */
	@Override
	HttpEndPoint getSessionEndPoint();
}
