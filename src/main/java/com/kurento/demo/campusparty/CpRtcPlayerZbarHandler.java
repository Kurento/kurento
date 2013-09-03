package com.kurento.demo.campusparty;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.PlayRequest;
import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.PlayerService;
import com.kurento.kmf.content.internal.player.PlayRequestImpl;
import com.kurento.kmf.content.jsonrpc.JsonRpcEvent;
import com.kurento.kmf.media.MediaEventListener;
import com.kurento.kmf.media.ZBarEvent;

@PlayerService(name = "CpRtcPlayerZbarHandler", path = "/cpRtcPlayerZbar", useControlProtocol = true)
public class CpRtcPlayerZbarHandler implements PlayerHandler {

	@Override
	public void onPlayRequest(final PlayRequest playRequest)
			throws ContentException {
		if (CpRtcRtpZbarHandler.sharedFilterReference == null) {
			playRequest.reject(500, "Rtp session has not been established");
			return;
		}

		CpRtcRtpZbarHandler.sharedFilterReference
				.addListener(new MediaEventListener<ZBarEvent>() {

					@Override
					public void onEvent(ZBarEvent event) {
						((PlayRequestImpl) playRequest)
								.produceEvents(JsonRpcEvent.newEvent(
										event.getType(), event.getValue()));
					}
				});

		playRequest.play(CpRtcRtpZbarHandler.sharedFilterReference);

	}

	@Override
	public void onContentPlayed(PlayRequest playRequest) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onContentError(PlayRequest playRequest,
			ContentException exception) {
		// TODO Auto-generated method stub

	}

}
