package com.kurento.demo.campusparty;

import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;

@HttpPlayerService(name = "CpRtcPlayerHandler", path = "/cpRtcPlayerJack", redirect = true, useControlProtocol = true)
public class CpRtcPlayerJackHandler extends HttpPlayerHandler {

	@Override
	public void onContentRequest(HttpPlayerSession session) throws Exception {
		if (CpRtcRtpJackHandler.sharedFilterReference == null) {
			session.terminate(500, "Rtp session has not been established");
		} else {
			session.start(CpRtcRtpJackHandler.sharedFilterReference);
		}
	}

}
