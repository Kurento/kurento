package com.kurento.demo;

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
