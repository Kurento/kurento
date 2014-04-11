/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
package com.kurento.test.repository;

import java.util.NoSuchElementException;

import com.kurento.kmf.content.HttpRecorderHandler;
import com.kurento.kmf.content.HttpRecorderService;
import com.kurento.kmf.content.HttpRecorderSession;
import com.kurento.kmf.repository.Repository;
import com.kurento.kmf.repository.RepositoryItem;

/**
 * HTTP Recorder in Repository; tunnel strategy (redirect=false, by default);
 * not using JSON-RPC control protocol (useControlProtocol=false).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.1.1
 */
@HttpRecorderService(path = "/recorderTunnelRepository", useControlProtocol = false)
public class RecorderTunnelRepository extends HttpRecorderHandler {

	@Override
	public void onContentRequest(HttpRecorderSession contentSession)
			throws Exception {
		final String itemId = "itemTunnel";
		Repository repository = contentSession.getRepository();
		RepositoryItem repositoryItem;
		try {
			repositoryItem = repository.findRepositoryItemById(itemId);
			getLogger().info("Deleting existing repository '{}'", itemId);
			repository.remove(repositoryItem);
		} catch (NoSuchElementException e) {
			getLogger().info(
					"Repository item '{}' does not previously exist ({})",
					itemId, e.getMessage());
		}
		repositoryItem = contentSession.getRepository().createRepositoryItem(
				itemId);
		getLogger().info("Created repository item {}", repositoryItem);
		contentSession.start(repositoryItem);
		getLogger().info("Session started {}", contentSession.getSessionId());
	}

}
