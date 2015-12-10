/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.kurento.basicroom;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.kurento.client.Continuation;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.internal.server.KurentoServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonObject;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 1.0.0
 */
public class RoomParticipant implements Closeable {

	private static final Logger log = LoggerFactory
			.getLogger(RoomParticipant.class);

	private final String name;
	private final Room room;

	private final WebSocketSession session;
	private final MediaPipeline pipeline;

	private WebRtcEndpoint receivingEndpoint;
	private final ConcurrentMap<String, WebRtcEndpoint> sendingEndpoints = new ConcurrentHashMap<>();

	private BlockingQueue<String> messages = new ArrayBlockingQueue<>(10);
	private Thread senderThread;

	private volatile boolean closed;

	public RoomParticipant(String name, Room room, WebSocketSession session,
			MediaPipeline pipeline) {

		this.pipeline = pipeline;
		this.name = name;
		this.session = session;
		this.room = room;
		this.receivingEndpoint = new WebRtcEndpoint.Builder(pipeline).build();

		this.senderThread = new Thread("sender:" + name) {
			public void run() {
				try {
					internalSendMessage();
				} catch (InterruptedException e) {
					return;
				}
			}
		};

		this.senderThread.start();
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
	public WebSocketSession getSession() {
		return session;
	}

	public WebRtcEndpoint getReceivingEndpoint() {
		return receivingEndpoint;
	}

	/**
	 * The room to which the user is currently attending
	 *
	 * @return The room
	 */
	public Room getRoom() {
		return this.room;
	}

	/**
	 * @param sender
	 * @param sdpOffer
	 * @throws IOException
	 */
	public void receiveVideoFrom(RoomParticipant sender, String sdpOffer) {

		log.info("USER {}: Request to receive video from {} in room {}",
				this.name, sender.getName(), this.room.getName());

		log.trace("USER {}: SdpOffer for {} is {}", this.name,
				sender.getName(), sdpOffer);

		final String ipSdpAnswer = this.createSdpResponseForUser(sender,
				sdpOffer);

		if (ipSdpAnswer != null) {
			final JsonObject scParams = new JsonObject();
			scParams.addProperty("id", "receiveVideoAnswer");
			scParams.addProperty("name", sender.getName());
			scParams.addProperty("sdpAnswer", ipSdpAnswer);

			log.trace("USER {}: SdpAnswer for {} is {}", this.name,
					sender.getName(), ipSdpAnswer);
			this.sendMessage(scParams);
		}
	}

	private String createSdpResponseForUser(RoomParticipant sender,
			String sdpOffer) {

		WebRtcEndpoint receivingEndpoint = sender.getReceivingEndpoint();
		if (receivingEndpoint == null) {
			log.warn(
					"PARTICIPANT {}: Trying to connect to a user without receiving endpoint (it seems is not yet fully connected)",
					this.name);
			return null;
		}

		if (sender.getName().equals(name)) {
			// FIXME: Use another message type for receiving sdp offer
			log.debug("PARTICIPANT {}: configuring loopback", this.name);
			return receivingEndpoint.processOffer(sdpOffer);
		}

		if (sendingEndpoints.get(sender.getName()) != null) {
			log.warn(
					"PARTICIPANT {}: There is a sending endpoint to user {} when trying to create another one",
					this.name, sender.getName());
			return null;
		}

		log.debug("PARTICIPANT {}: Creating a sending endpoint to user {}",
				this.name, sender.getName());

		WebRtcEndpoint sendingEndpoint = new WebRtcEndpoint.Builder(pipeline)
				.build();
		WebRtcEndpoint oldSendingEndpoint = sendingEndpoints.putIfAbsent(
				sender.getName(), sendingEndpoint);

		if (oldSendingEndpoint != null) {
			log.warn(
					"PARTICIPANT {}: Two threads have created at the same time a sending endpoint for user {}",
					this.name, sender.getName());
			return null;
		}

		log.debug("PARTICIPANT {}: Created sending endpoint for user {}",
				this.name, sender.getName());
		try {
			receivingEndpoint = sender.getReceivingEndpoint();
			if (receivingEndpoint != null) {
				receivingEndpoint.connect(sendingEndpoint);
				return sendingEndpoint.processOffer(sdpOffer);
			}

		} catch (KurentoServerException e) {

			// TODO Check object status when KurentoClient set this info in the
			// object
			if (e.getCode() == 40101) {
				log.warn(
						"Receiving endpoint is released when trying to connect a sending endpoint to it",
						e);
			} else {
				log.error(
						"Exception connecting receiving endpoint to sending endpoint",
						e);
				sendingEndpoint.release(new Continuation<Void>() {
					@Override
					public void onSuccess(Void result) throws Exception {

					}

					@Override
					public void onError(Throwable cause) throws Exception {
						log.error("Exception releasing WebRtcEndpoint", cause);
					}
				});
			}

			sendingEndpoints.remove(sender.getName());

			releaseEndpoint(sender.getName(), sendingEndpoint);
		}

		return null;
	}

	/**
	 * @param sender
	 *            the participant
	 */
	public void cancelSendingVideoTo(final RoomParticipant sender) {
		this.cancelSendingVideoTo(sender.getName());
	}

	/**
	 * @param senderName
	 *            the participant
	 */
	public void cancelSendingVideoTo(final String senderName) {

		log.debug("PARTICIPANT {}: canceling video sending to {}", this.name,
				senderName);

		final WebRtcEndpoint sendingEndpoint = sendingEndpoints
				.remove(senderName);

		if (sendingEndpoint == null) {
			log.warn(
					"PARTICIPANT {}: Trying to cancel sending video to user {}. But there is no such sending endpoint",
					this.name, senderName);
		} else {

			log.debug("PARTICIPANT {}: Cancelling sending endpoint to user {}",
					this.name, senderName);

			releaseEndpoint(senderName, sendingEndpoint);
		}
	}

	private void releaseEndpoint(final String senderName,
			final WebRtcEndpoint sendingEndpoint) {
		sendingEndpoint.release(new Continuation<Void>() {
			@Override
			public void onSuccess(Void result) throws Exception {
				log.debug(
						"PARTICIPANT {}: Released successfully incoming EP for {}",
						RoomParticipant.this.name, senderName);
			}

			@Override
			public void onError(Throwable cause) throws Exception {
				log.warn("PARTICIPANT " + RoomParticipant.this.name
						+ ": Could not release sending endpoint for user "
						+ senderName, cause);
			}
		});
	}

	@Override
	public void close() {
		log.debug("PARTICIPANT {}: Closing user", this.name);

		this.closed = true;

		for (final String remoteParticipantName : sendingEndpoints.keySet()) {

			log.debug("PARTICIPANT {}: Released incoming EP for {}", this.name,
					remoteParticipantName);

			final WebRtcEndpoint ep = this.sendingEndpoints
					.get(remoteParticipantName);

			releaseEndpoint(remoteParticipantName, ep);
		}

		if (receivingEndpoint != null) {
			releaseEndpoint(name, receivingEndpoint);
			receivingEndpoint = null;
		}

		senderThread.interrupt();
	}

	public void sendMessage(JsonObject message) {
		log.debug("USER {}: Enqueueing message {}", name, message);
		try {
			messages.put(message.toString());
			log.debug("USER {}: Enqueued message {}", name, message);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void internalSendMessage() throws InterruptedException {
		while (true) {
			try {
				String message = messages.take();
				log.debug("Sending message {} to user {}", message,
						RoomParticipant.this.name);
				RoomParticipant.this.session.sendMessage(new TextMessage(
						message));
			} catch (InterruptedException e) {
				return;
			} catch (Exception e) {
				log.warn("Exception while sending message to user '"
						+ RoomParticipant.this.name + "'", e);
			}
		}
	}

	public boolean isClosed() {
		return closed;
	}

	@Override
	public String toString() {
		return "[User: " + name + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((room == null) ? 0 : room.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RoomParticipant other = (RoomParticipant) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (room == null) {
			if (other.room != null)
				return false;
		} else if (!room.equals(other.room))
			return false;
		return true;
	}
}
