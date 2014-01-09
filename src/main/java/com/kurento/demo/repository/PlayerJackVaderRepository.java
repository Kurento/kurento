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

import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.JackVaderFilter;
import com.kurento.kmf.media.MediaApiConfiguration;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.repository.RepositoryHttpPlayer;
import com.kurento.kmf.repository.RepositoryItem;

/**
 * HTTP Player of previously recorded contents in Repository (contenId will be
 * the repository item Id) plus a filter (JackVaderFilter); tunnel strategy
 * (redirect=false, by default); not using JSON-RPC control protocol
 * (useControlProtocol=false).
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.1
 */
@HttpPlayerService(path = "/playerJackVaderRepository/*", useControlProtocol = false)
public class PlayerJackVaderRepository extends HttpPlayerHandler {

	@Autowired
	private MediaApiConfiguration config;

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
			RepositoryHttpPlayer player = repositoryItem
					.createRepositoryHttpPlayer();
			String mediaUrl = contentSession.getHttpServletRequest()
					.getScheme()
					+ "://"
					+ config.getHandlerAddress()
					+ ":"
					+ contentSession.getHttpServletRequest().getServerPort()
					+ player.getURL();
			getLogger().info("mediaUrl {}", mediaUrl);

			MediaPipeline mp = contentSession.getMediaPipelineFactory()
					.create();
			contentSession.releaseOnTerminate(mp);
			PlayerEndpoint playerEndpoint = mp.newPlayerEndpoint(mediaUrl)
					.build();
			JackVaderFilter filter = mp.newJackVaderFilter().build();
			playerEndpoint.connect(filter);
			contentSession.setAttribute("player", playerEndpoint);
			contentSession.start(filter);
		}
	}

	@Override
	public void onContentStarted(HttpPlayerSession session) {
		PlayerEndpoint PlayerEndpoint = (PlayerEndpoint) session
				.getAttribute("player");
		PlayerEndpoint.play();
	}

}
