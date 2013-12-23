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

import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;

@HttpPlayerService(name = "SimplePlayerHandler", path = "/playerHttp/*", redirect = true, useControlProtocol = false)
public class PlayerHttpHandler extends HttpPlayerHandler {

	@Override
	public void onContentRequest(HttpPlayerSession session) throws Exception {
		if (session.getContentId() != null
				&& session.getContentId().toLowerCase().startsWith("bar")) {
			session.start("https://ci.kurento.com/video/barcodes.webm");
		} else if (session.getContentId() != null
				&& session.getContentId().toLowerCase()
						.endsWith("fiwarecut.webm")) {
			session.start("https://ci.kurento.com/video/fiwarecut.webm");
		} else {
			session.start("http://media.w3.org/2010/05/sintel/trailer.webm");
		}
	}

}
