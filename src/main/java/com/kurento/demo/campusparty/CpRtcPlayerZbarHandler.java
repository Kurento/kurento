package com.kurento.demo.campusparty;

import com.kurento.kmf.content.ContentEvent;
import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.events.CodeFoundEvent;
import com.kurento.kmf.media.events.MediaEventListener;

@HttpPlayerService(name = "CpRtcPlayerZbarHandler", path = "/cpRtcPlayerZbar", redirect = true, useControlProtocol = true)
public class CpRtcPlayerZbarHandler extends HttpPlayerHandler {

	@Override
	public void onContentRequest(final HttpPlayerSession session)
			throws Exception {
		if (CpRtcRtpZbarHandler.sharedFilterReference == null) {
			session.terminate(500, "Rtp session has not been established");
			return;
		}
		CpRtcRtpZbarHandler.sharedFilterReference
				.addCodeFoundDataListener(new MediaEventListener<CodeFoundEvent>() {

					@Override
					public void onEvent(CodeFoundEvent event) {
						session.publishEvent(new ContentEvent(event.getType(),
								event.getValue()));
					}
				});

		session.start(CpRtcRtpZbarHandler.sharedFilterReference);
	}

}
