package com.kurento.kmf.media;

import java.io.IOException;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.media.PlayerEvent.PlayerEventType;
import com.kurento.kms.api.MediaEvent;
import com.kurento.kms.api.MediaObjectId;
import com.kurento.kms.api.PlayerEndPointEvent;
import com.kurento.kms.api.UriEndPointType;

public class PlayerEndPoint extends UriEndPoint {

	public static final Logger log = LoggerFactory
			.getLogger(PlayerEndPoint.class);

	private static final long serialVersionUID = 1L;

	static final UriEndPointType uriEndPointType = UriEndPointType.PLAYER_END_POINT;

	PlayerEndPoint(MediaObjectId playerEndPointId) {
		super(playerEndPointId);
	}

	@Override
	KmsEvent deserializeEvent(MediaEvent event) {
		try {
			TProtocol prot = handler.getProtocolFromEvent(event);

			PlayerEndPointEvent playerEvent = new PlayerEndPointEvent();
			playerEvent.read(prot);

			if (playerEvent.isSetEos())
				return new PlayerEvent(this, PlayerEventType.EOS);

			log.error("Unknown player event type, falling back to default deserializer");
		} catch (TException e) {
			log.error("Error deserializing player event: " + e, e);
		}

		return super.deserializeEvent(event);
	}

	public MediaEventListener<PlayerEvent> addListener(
			MediaEventListener<PlayerEvent> listener) {
		return handler.addListener(this, listener);
	}

	public boolean removeListener(MediaEventListener<PlayerEvent> listener) {
		return handler.removeListener(this, listener);
	}

	/* SYNC */

	public void play() throws IOException {
		start();
	}

	/* ASYNC */

	public void play(Continuation<Void> cont) throws IOException {
		start(cont);
	}
}
