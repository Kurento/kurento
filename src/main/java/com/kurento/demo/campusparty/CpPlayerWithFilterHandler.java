package com.kurento.demo.campusparty;

import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.JackVaderFilter;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.PlayerEndPoint;

@HttpPlayerService(name = "CpPlayerWithJackVaderFilter", path = "/cpPlayerJack", redirect = true, useControlProtocol = true)
public class CpPlayerWithFilterHandler extends HttpPlayerHandler {

	@Override
	public void onContentRequest(HttpPlayerSession session) throws Exception {
		MediaPipelineFactory mpf = session.getMediaPipelineFactory();
		MediaPipeline mp = mpf.create();
		session.releaseOnTerminate(mp);

		PlayerEndPoint playerEndPoint = mp
				.createPlayerEndPoint("https://ci.kurento.com/video/fiwarecut.webm");

		JackVaderFilter filter = mp.createJackVaderFilter();
		playerEndPoint.connect(filter);
		session.setAttribute("player", playerEndPoint);
		session.start(filter);
	}

	@Override
	public void onContentStarted(HttpPlayerSession session) {
		PlayerEndPoint playerendPoint = (PlayerEndPoint) session
				.getAttribute("player");
		playerendPoint.play();
	}

}
