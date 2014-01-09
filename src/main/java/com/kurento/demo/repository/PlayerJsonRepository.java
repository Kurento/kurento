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
package com.kurento.demo.repository;

import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.repository.RepositoryItem;

/**
 * HTTP Player of previously recorded contents in Repository (contenId will be
 * the repository item Id); tunnel strategy (redirect=false, by default); using
 * JSON-RPC control protocol (useControlProtocol=true).
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.1
 */
@HttpPlayerService(path = "/playerJsonRepository/*", useControlProtocol = true)
public class PlayerJsonRepository extends HttpPlayerHandler {

	@Override
	public void onContentRequest(HttpPlayerSession contentSession)
			throws Exception {
		String contentId = contentSession.getContentId();
		RepositoryItem repositoryItem = contentSession.getRepository()
				.findRepositoryItemById(contentId);
		if (repositoryItem == null) {
			String message = "Repository item " + contentId + " does no exist";
			getLogger().warn(message);
			contentSession.terminate(404, message);
		} else {
			contentSession.start(repositoryItem);
		}
	}

}
