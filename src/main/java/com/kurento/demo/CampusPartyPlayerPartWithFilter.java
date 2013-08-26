package com.kurento.demo;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.PlayRequest;
import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.PlayerService;
import com.kurento.kmf.content.internal.player.PlayRequestImpl;
import com.kurento.kmf.content.jsonrpc.JsonRpcEvent;
import com.kurento.kmf.media.MediaEventListener;
import com.kurento.kmf.media.ZBarEvent;

@PlayerService(name = "", path = "/campusPartyPlayerFilter", useControlProtocol = true, redirect = false)
public class CampusPartyPlayerPartWithFilter implements PlayerHandler {

	@Override
	public void onPlayRequest(final PlayRequest playRequest)
			throws ContentException {

		if (CampusPartyRtpPartWithFilter.zBarFilterStaticReference != null) {
			playRequest
					.play(CampusPartyRtpPartWithFilter.zBarFilterStaticReference);
			playRequest.setAttribute("lastEventValue", "");
			CampusPartyRtpPartWithFilter.zBarFilterStaticReference
					.addListener(new MediaEventListener<ZBarEvent>() {
						@Override
						public void onEvent(ZBarEvent event) {
							String lastEventValue = (String) playRequest
									.getAttribute("lastEventValue");
							if (lastEventValue.equals(event.getValue())) {
								return;
							}
							playRequest.setAttribute("lastEventValue",
									event.getValue());
							((PlayRequestImpl) playRequest)
									.produceEvents(JsonRpcEvent.newEvent(
											event.getType(), event.getValue()));
							System.out.println("Event received "
									+ event.getValue());

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
