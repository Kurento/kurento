package com.kurento.demo;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.PlayRequest;
import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.PlayerService;
import com.kurento.kmf.content.internal.player.PlayRequestImpl;
import com.kurento.kmf.content.jsonrpc.JsonRpcEvent;
import com.kurento.kmf.media.MediaEventListener;
import com.kurento.kmf.media.ZBarEvent;

@PlayerService(name = "", path = "/campusPartyPlayerFilter", useControlProtocol = false, redirect = false)
public class CampusPartyPlayerPartWithFilter implements PlayerHandler {

	@Override
	public void onPlayRequest(final PlayRequest playRequest)
			throws ContentException {
		if (CampusPartyRtpPartWithFilter.zbarFilter != null) {
			playRequest.play(CampusPartyRtpPartWithFilter.zbarFilter);
			CampusPartyRtpPartWithFilter.zbarFilter
					.addListener(new MediaEventListener<ZBarEvent>() {
						@Override
						public void onEvent(ZBarEvent event) {
							((PlayRequestImpl) playRequest)
									.produceEvents(JsonRpcEvent.newEvent(
											event.getType(), event.getValue()));

						}
					});
		} else {
			playRequest.reject(500, "No rtp session found");
		}

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
