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
package com.kurento.demo.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.kurento.demo.player.PlayerRedirect;
import com.kurento.demo.player.PlayerTunnel;
import com.kurento.demo.playerjson.PlayerJsonRedirect;
import com.kurento.demo.playerjson.PlayerJsonTunnel;
import com.kurento.kmf.content.ContentEvent;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.JackVaderFilter;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.PlayerEndPoint;
import com.kurento.kmf.media.ZBarFilter;
import com.kurento.kmf.media.events.CodeFoundEvent;
import com.kurento.kmf.media.events.MediaEventListener;

/**
 * Static class which contains a generic implementation of an HTTP Player,
 * selecting the video to be played depending on the <code>contentId</code>.
 * This static code will be used in {@link PlayerRedirect}, {@link PlayerTunnel}
 * , {@link PlayerJsonRedirect} and {@link PlayerJsonTunnel}.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
public class GenericPlayer {

	public static final Log log = LogFactory.getLog(GenericPlayer.class);

	public static void play(final HttpPlayerSession session) {
		// contendId discriminates between a termination or a play. In case of
		// the play, contentId selects the URL and filter to be employed
		String contentId = session.getContentId();

		if (contentId != null && contentId.equalsIgnoreCase("reject")) {
			session.terminate(407, "Reject in player handler");
		} else {
			// Small video in WEBM by default (small.webm)
			String url = VideoURLs.map.get("small-webm");
			if (contentId != null && VideoURLs.map.containsKey(contentId)) {
				url = VideoURLs.map.get(contentId);
			}
			if (contentId != null && contentId.equalsIgnoreCase("jack")) {
				// Jack Vader Filter
				MediaPipelineFactory mpf = session.getMediaPipelineFactory();
				MediaPipeline mp = mpf.create();
				session.releaseOnTerminate(mp);
				PlayerEndPoint playerEndPoint = mp.newPlayerEndPoint(url)
						.build();
				JackVaderFilter filter = mp.newJackVaderFilter().build();
				playerEndPoint.connect(filter);
				session.setAttribute("player", playerEndPoint);
				session.start(filter);
			} else if (contentId != null && contentId.equalsIgnoreCase("zbar")) {
				// ZBar Filter
				MediaPipelineFactory mpf = session.getMediaPipelineFactory();
				MediaPipeline mp = mpf.create();
				PlayerEndPoint player = mp.newPlayerEndPoint(url).build();
				session.setAttribute("player", player);
				ZBarFilter zBarFilter = mp.newZBarFilter().build();
				player.connect(zBarFilter);
				session.start(zBarFilter);
				session.setAttribute("eventValue", "");
				zBarFilter
						.addCodeFoundDataListener(new MediaEventListener<CodeFoundEvent>() {
							@Override
							public void onEvent(CodeFoundEvent event) {
								log.info("Code Found " + event.getValue());
								if (session.getAttribute("eventValue")
										.toString().equals(event.getValue())) {
									return;
								}
								session.setAttribute("eventValue",
										event.getValue());
								session.publishEvent(new ContentEvent(event
										.getType(), event.getValue()));
							}
						});
			}

			else {
				// Player without filter
				session.start(url);
			}
		}
	}
}
