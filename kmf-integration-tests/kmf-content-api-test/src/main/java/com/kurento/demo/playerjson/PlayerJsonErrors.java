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
package com.kurento.demo.playerjson;

import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;

/**
 * HTTP Player Handler; tunnel strategy; JSON control protocol; depending on the
 * value of the contentId, this handlers end with error (terminating the session
 * of raising an exception).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.1.1
 */
@HttpPlayerService(path = "/playerErrors/*", redirect = false, useControlProtocol = true)
public class PlayerJsonErrors extends HttpPlayerHandler {

	@Override
	public void onContentRequest(final HttpPlayerSession contentSession)
			throws Exception {
		final String contentId = contentSession.getContentId();
		if ("exception".equalsIgnoreCase(contentId)) {
			throw new Exception("Custom exception in handler");
		} else {
			contentSession.terminate(503,
					"Custom session termination in handler");
		}
	}

}
