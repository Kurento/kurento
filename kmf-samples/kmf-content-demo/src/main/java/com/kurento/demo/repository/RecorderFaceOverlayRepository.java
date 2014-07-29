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

import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.content.HttpRecorderHandler;
import com.kurento.kmf.content.HttpRecorderService;
import com.kurento.kmf.content.HttpRecorderSession;
import com.kurento.kmf.media.FaceOverlayFilter;
import com.kurento.kmf.media.HttpPostEndpoint;
import com.kurento.kmf.media.MediaApiConfiguration;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.RecorderEndpoint;
import com.kurento.kmf.repository.Repository;
import com.kurento.kmf.repository.RepositoryHttpRecorder;
import com.kurento.kmf.repository.RepositoryItem;

/**
 * HTTP Recorder in Repository using the FaceOverlay filter previous to
 * recording; tunnel strategy (redirect=false, by default); not using JSON-RPC
 * control protocol (useControlProtocol=false).
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.1
 */
@HttpRecorderService(path = "/recorderFaceOverlayRepository", useControlProtocol = false)
public class RecorderFaceOverlayRepository extends HttpRecorderHandler {

	@Autowired
	private MediaApiConfiguration config;

	@Override
	public void onContentRequest(HttpRecorderSession contentSession)
			throws Exception {
		Repository repository = contentSession.getRepository();
		RepositoryItem repositoryItem;
		String itemId = "itemFaceOverlay";
		try {
			repositoryItem = repository.findRepositoryItemById(itemId);
			getLogger().info("Deleting existing repository '{}'", itemId);
			repository.remove(repositoryItem);
		} catch (NoSuchElementException e) {
			getLogger().info("Repository item '{}' does not previously exist",
					itemId);
		}

		repositoryItem = contentSession.getRepository().createRepositoryItem(
				itemId);
		RepositoryHttpRecorder recorder = repositoryItem
				.createRepositoryHttpRecorder();
		String mediaUrl = contentSession.getHttpServletRequest().getScheme()
				+ "://" + config.getHandlerAddress() + ":"
				+ contentSession.getHttpServletRequest().getServerPort()
				+ recorder.getURL();

		MediaPipeline mp = contentSession.getMediaPipelineFactory().create();
		contentSession.releaseOnTerminate(mp);

		RecorderEndpoint recorderEndPoint = mp.newRecorderEndpoint(mediaUrl)
				.build();

		final FaceOverlayFilter filter = mp.newFaceOverlayFilter().build();
		filter.setOverlayedImage(
				"http://files.kurento.org/imgs/mario-wings.png", -0.35F, -1.2F,
				1.6F, 1.6F);

		filter.connect(recorderEndPoint);
		contentSession.setAttribute("recorder", recorderEndPoint);
		HttpPostEndpoint httpEndpoint = mp.newHttpPostEndpoint().build();
		httpEndpoint.connect(filter);
		contentSession.start(httpEndpoint);
	}

	@Override
	public void onContentStarted(HttpRecorderSession contentSession) {
		RecorderEndpoint recorderEndPoint = (RecorderEndpoint) contentSession
				.getAttribute("recorder");
		recorderEndPoint.record();
	}

	@Override
	public void onSessionTerminated(HttpRecorderSession contentSession,
			int code, String reason) throws Exception {
		RecorderEndpoint recorderEndPoint = (RecorderEndpoint) contentSession
				.getAttribute("recorder");
		recorderEndPoint.stop();
		super.onSessionTerminated(contentSession, code, reason);
	}
}
