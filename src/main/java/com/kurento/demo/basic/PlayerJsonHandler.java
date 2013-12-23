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
package com.kurento.demo.basic;

import com.kurento.kmf.content.ContentEvent;
import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;

@HttpPlayerService(name = "JsonPlayerHandler", path = "/playerJson/*", redirect = false, useControlProtocol = true)
public class PlayerJsonHandler extends HttpPlayerHandler {

	@Override
	public void onContentRequest(final HttpPlayerSession session)
			throws Exception {

		if (session.getContentId() != null
				&& session.getContentId().toLowerCase().startsWith("bar")) {
			session.start("https://ci.kurento.com/video/barcodes.webm");
		} else {
			session.start("http://media.w3.org/2010/05/sintel/trailer.webm");
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < 100; i++) {
					try {
						Thread.sleep(7000);
					} catch (InterruptedException e) {
					}
					if (i % 3 == 0) {
						session.publishEvent(new ContentEvent(
								"test-event-type", "test-event-data"));
					} else if (i % 3 == 1) {
						session.publishEvent(new ContentEvent("url-event-type",
								"http://www.urjc.es"));
					} else {
						session.publishEvent(new ContentEvent("url-event-type",
								"http://www.gsyc.es/"));
					}
				}
			}
		}).start();
	}
}
