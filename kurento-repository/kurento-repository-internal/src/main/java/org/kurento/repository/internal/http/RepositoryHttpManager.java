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

package org.kurento.repository.internal.http;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.kurento.commons.SecretGenerator;
import org.kurento.repository.RepositoryApiConfiguration;
import org.kurento.repository.RepositoryHttpEndpoint;
import org.kurento.repository.RepositoryHttpPlayer;
import org.kurento.repository.RepositoryHttpRecorder;
import org.kurento.repository.RepositoryItem;
import org.kurento.repository.internal.RepositoryHttpEndpointImpl;
import org.kurento.repository.internal.RepositoryHttpPlayerImpl;
import org.kurento.repository.internal.RepositoryHttpRecorderImpl;

@Component
public class RepositoryHttpManager {

	@Autowired
	private RepositoryApiConfiguration config;

	private String webappPublicURL;

	private String servletPath;

	private final ConcurrentMap<String, RepositoryHttpEndpointImpl> sessions = new ConcurrentHashMap<>();

	private final SecretGenerator generator = new SecretGenerator();

	@Autowired
	@Qualifier("repositoryTaskScheduler")
	private TaskScheduler scheduler;

	public RepositoryHttpPlayer createRepositoryHttpPlayer(
			RepositoryItem repositoryItem) {
		return (RepositoryHttpPlayer) createRepositoryHttpElem(repositoryItem,
				RepositoryHttpPlayer.class, null);
	}

	public RepositoryHttpRecorder createRepositoryHttpRecorder(
			RepositoryItem repositoryItem) {
		return (RepositoryHttpRecorder) createRepositoryHttpElem(
				repositoryItem, RepositoryHttpRecorder.class, null);
	}

	public RepositoryHttpPlayer createRepositoryHttpPlayer(
			RepositoryItem repositoryItem, String sessionIdInURL) {
		return (RepositoryHttpPlayer) createRepositoryHttpElem(repositoryItem,
				RepositoryHttpPlayer.class, sessionIdInURL);
	}

	public RepositoryHttpRecorder createRepositoryHttpRecorder(
			RepositoryItem repositoryItem, String sessionIdInURL) {
		return (RepositoryHttpRecorder) createRepositoryHttpElem(
				repositoryItem, RepositoryHttpRecorder.class, sessionIdInURL);
	}

	private RepositoryHttpEndpointImpl createRepositoryHttpElem(
			RepositoryItem repositoryItem,
			Class<? extends RepositoryHttpEndpoint> repoItemHttpElemClass,
			String sessionIdInURL) {

		if (sessionIdInURL == null) {
			sessionIdInURL = createUniqueId();
		}

		String url = createUlr(sessionIdInURL);

		RepositoryHttpEndpointImpl elem = null;

		if (repoItemHttpElemClass == RepositoryHttpPlayer.class) {
			elem = new RepositoryHttpPlayerImpl(repositoryItem, sessionIdInURL,
					url, this);
		} else {
			elem = new RepositoryHttpRecorderImpl(repositoryItem,
					sessionIdInURL, url, this);
		}

		sessions.put(sessionIdInURL, elem);

		return elem;
	}

	private String createUniqueId() {
		return generator.nextSecret();
	}

	private String createUlr(String sessionId) {
		return webappPublicURL + getDispatchURL(sessionId);
	}

	public String getDispatchURL(String id) {
		return servletPath + id;
	}

	public RepositoryHttpEndpointImpl getHttpRepoItemElem(String sessionId) {
		return (sessionId == null) ? null : sessions.get(sessionId);
	}

	public TaskScheduler getScheduler() {
		return scheduler;
	}

	public void disposeHttpRepoItemElem(String sessionId) {
		sessions.remove(sessionId);
	}

	public void disposeHttpRepoItemElemByItemId(RepositoryItem item,
			String message) {

		// We don't use another map indexed by RepositoryItemIds for several
		// reasons:
		// * Memory consumption
		// * More complex code (development time, difficult to maintain and
		// test)
		// * It is very unlike this operation is called in a reasonable use case

		Iterator<Entry<String, RepositoryHttpEndpointImpl>> it = sessions
				.entrySet().iterator();

		while (it.hasNext()) {
			Entry<String, RepositoryHttpEndpointImpl> entry = it.next();
			RepositoryHttpEndpointImpl elem = entry.getValue();
			if (elem.getRepositoryItem().getId().equals(item.getId())) {
				elem.forceStopHttpManager(message);
				it.remove();
			}
		}
	}

	public void setWebappPublicURL(String webappURL) {
		this.webappPublicURL = webappURL;
	}

	public void setServletPath(String servletPath) {
		this.servletPath = servletPath;
	}

}
