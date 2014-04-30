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
package com.kurento.demo.cpbrazil;

import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;

/**
 * HTTP Player of previously recorded WebRTC content; tunnel strategy
 * (redirect=true); not using JSON-RPC control protocol
 * (useControlProtocol=false).
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.1
 */
@HttpPlayerService(path = "/cpbPlayer/*", useControlProtocol = false, redirect = true)
public class CpbPlayer extends HttpPlayerHandler {

	@Override
	public void onContentRequest(HttpPlayerSession session) throws Exception {
		String contentId = session.getContentId();
		session.start("file:///tmp/" + contentId);
	}

}