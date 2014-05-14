package com.kurento.kmf.phone;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.kurento.kmf.jsonrpcconnector.Session;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.WebRtcEndpoint;

public class Participant {

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
		WebRtcEndpoint incoming = incomingMedia.get(sender.getName());

		if (incoming == null) {
			incoming = pipeline.newWebRtcEndpoint().build();
			incomingMedia.put(sender.getName(), incoming);
		}

		// TODO maybe this doesn't go here
		sender.getOutgoingWebRtcPeer().connect(incoming);

		return incoming;
	}
}
