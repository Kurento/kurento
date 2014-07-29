package com.kurento.kmf.demo.group;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.jsonrpcconnector.Session;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.Endpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.WebRtcEndpoint;

public class Participant implements Closeable {

	private static final Logger log = LoggerFactory
			.getLogger(Participant.class);

	private final String name;
	private final Session session;

	private final WebRtcEndpoint outgoingMedia;
	private final ConcurrentMap<String, WebRtcEndpoint> incomingMedia = new ConcurrentHashMap<>();
	private final MediaPipeline pipeline;

	public Participant(String name, Session session, MediaPipeline pipeline) {
		this.pipeline = pipeline;
		this.name = name;
		this.session = session;
		this.outgoingMedia = this.pipeline.newWebRtcEndpoint().build();
	}

	public WebRtcEndpoint getOutgoingWebRtcPeer() {
		return outgoingMedia;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the session
	 */
	public Session getSession() {
		return session;
	}

	/**
	 * @param sender
	 *            the participant
	 * @return the endpoint used to receive media from a certain participant
	 */
	public WebRtcEndpoint receiveVideoFrom(Participant sender) {
		if (sender.getName().equals(name)) {
			log.debug("PARTICIPANT {}: configuring loopback", this.name);
			// Filter filter = this.pipeline.newFaceOverlayFilter().build();
			// outgoingMedia.connect(filter);
			// filter.connect(outgoingMedia);
			return outgoingMedia;
		}
		log.debug("PARTICIPANT {}: receiving video from {}", this.name,
				sender.getName());

		WebRtcEndpoint incoming = incomingMedia.get(sender.getName());

		if (incoming == null) {
			log.debug("PARTICIPANT {}: creating new endpoint for {}",
					this.name, sender.getName());
			incoming = pipeline.newWebRtcEndpoint().build();
			incomingMedia.put(sender.getName(), incoming);
		}

		log.debug("PARTICIPANT {}: obtained endpoint for {}", this.name,
				sender.getName());
		sender.getOutgoingWebRtcPeer().connect(incoming);

		return incoming;
	}

	/**
	 * @param sender
	 *            the participant
	 */
	public void cancelVideoFrom(final Participant sender) {
		this.cancelVideoFrom(sender.getName());
	}

	/**
	 * @param senderName
	 *            the participant
	 */
	public void cancelVideoFrom(final String senderName) {
		log.debug("PARTICIPANT {}: canceling video reception from {}",
				this.name, senderName);
		WebRtcEndpoint incoming = incomingMedia.remove(senderName);

		log.debug("PARTICIPANT {}: removing endpoint for {}", this.name,
				senderName);
		incoming.release(new Continuation<Void>() {
			@Override
			public void onSuccess(Void result) throws Exception {
				log.trace(
						"PARTICIPANT {}: Released successfully incoming EP for {}",
						Participant.this.name, senderName);
			}

			@Override
			public void onError(Throwable cause) throws Exception {
				log.warn(
						"PARTICIPANT {}: Could not release incoming EP for {}",
						Participant.this.name, senderName);
			}
		});
	}

	@Override
	public void close() throws IOException {
		log.debug("PARTICIPANT {}: Releasing resources", this.name);
		for (final String remoteParticipantName : incomingMedia.keySet()) {

			log.trace("PARTICIPANT {}: Released incoming EP for {}", this.name,
					remoteParticipantName);
			Endpoint ep = this.incomingMedia.get(remoteParticipantName);
			ep.release(new Continuation<Void>() {

				@Override
				public void onSuccess(Void result) throws Exception {
					log.trace(
							"PARTICIPANT {}: Released successfully incoming EP for {}",
							Participant.this.name, remoteParticipantName);
				}

				@Override
				public void onError(Throwable cause) throws Exception {
					log.warn(
							"PARTICIPANT {}: Could not release incoming EP for {}",
							Participant.this.name, remoteParticipantName);
				}
			});
		}

		outgoingMedia.release(new Continuation<Void>() {

			@Override
			public void onSuccess(Void result) throws Exception {
				log.trace("PARTICIPANT {}: Released outgoing EP",
						Participant.this.name);
			}

			@Override
			public void onError(Throwable cause) throws Exception {
				log.warn("PARTICIPANT {}: Could not release outgoing EP",
						Participant.this.name);
			}
		});
	}

}
