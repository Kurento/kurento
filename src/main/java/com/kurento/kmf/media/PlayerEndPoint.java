package com.kurento.kmf.media;

import java.io.IOException;
import java.nio.BufferUnderflowException;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TMemoryBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.media.PlayerEvent.PlayerEventType;
import com.kurento.kms.api.MediaEvent;
import com.kurento.kms.api.PlayerEndPointEvent;
import com.kurento.kms.api.UriEndPointType;

public class PlayerEndPoint extends UriEndPoint {

	public static final Logger log = LoggerFactory
			.getLogger(PlayerEndPoint.class);

	private static final long serialVersionUID = 1L;

	static final UriEndPointType uriEndPointType = UriEndPointType.PLAYER_END_POINT;

	PlayerEndPoint(com.kurento.kms.api.MediaObject playerEndPoint) {
		super(playerEndPoint);
	}

	// TODO: Move this to a utilities class
	private TProtocol getProtocolFromEvent(MediaEvent event) throws TException {
		TMemoryBuffer tr = new TMemoryBuffer(event.event.remaining());
		TProtocol prot = new TBinaryProtocol(tr);

		byte data[] = new byte[event.event.remaining()];
		try {
			event.event.get(data);

			tr.write(data);

			return prot;
		} catch (BufferUnderflowException e) {
			log.error("Error deserializing event: " + e, e);
			throw new TException(e);
		}
	}

	@Override
	KmsEvent deserializeEvent(MediaEvent event) {
		try {
			TProtocol prot = getProtocolFromEvent(event);

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

	/* SYNC */

	public void play() throws IOException {
		start();
	}

	/* ASYNC */

	public void play(Continuation<Void> cont) throws IOException {
		start(cont);
	}
}
