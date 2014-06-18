package com.kurento.kmf.demo.group;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.Session;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;

public class ParticipantHandler extends DefaultJsonRpcHandler<JsonObject> {

	private static final Logger log = LoggerFactory
			.getLogger(ParticipantHandler.class);

	@Autowired
	private RoomManager roomManager;

	private Session session;

	private String name;

	private String roomName;

	private Participant myself;

	@Override
	public void handleRequest(Transaction transaction,
			Request<JsonObject> request) throws Exception {

		JsonObject params = request.getParams();

		switch (request.getMethod()) {
		case "joinRoom":
			joinRoom(transaction, params);
			sendParticipantNames(transaction);
			break;
		case "receiveVideoFrom":
			receiveVideo(transaction, params);
			break;
		case "leaveRoom":
			leaveRoom();
			break;
		default:
			break;
		}
	}

	@Override
	public void afterConnectionClosed(Session session, String status)
			throws Exception {
		leaveRoom();
	}

	private void leaveRoom() throws IOException {
		log.debug("PARTICIPANT {}: Leaving room {}", this.name, this.roomName);
		roomManager.getRoom(roomName).removeParticipant(name);
		myself.close();
		myself = null;
	}

	/**
	 * @param transaction
	 * @param params
	 * @throws IOException
	 */
	private void receiveVideo(Transaction transaction, JsonObject params)
			throws IOException {
		String senderName = params.get("sender").getAsString();
		@SuppressWarnings("resource")
		Participant sender = roomManager.getRoom(this.roomName).getParticipant(
				senderName);

		log.info("PARTICIPANT {}: connecting with {} in room {}", this.name,
				senderName, this.roomName);
		String ipSdpOffer = params.get("sdpOffer").getAsString();

		log.trace("PARTICIPANT {}: SdpOffer is {}", this.name, ipSdpOffer);

		String ipSdpAnswer = myself.receiveVideoFrom(sender).processOffer(
				ipSdpOffer);
		JsonObject scParams = new JsonObject();
		scParams.addProperty("sdpAnswer", ipSdpAnswer);

		log.trace("PARTICIPANT {}: SdpAnswer is {}", this.name, ipSdpAnswer);
		transaction.sendResponse(scParams);
	}

	@Override
	public void afterConnectionEstablished(Session participantSession)
			throws Exception {
		this.session = participantSession;
	}

	private void joinRoom(Transaction transaction, JsonObject params)
			throws IOException {

		this.roomName = params.get("room").getAsString();
		this.name = params.get("name").getAsString();

		Room room;
		log.info("PARTICIPANT {}: trying to join room {}", this.name,
				this.roomName);
		try {
			room = roomManager.getRoom(this.roomName);
			this.myself = room.join(this.name, session);
		} catch (IllegalArgumentException e) {
			// TODO don't send only the exception, add more info
			transaction.sendError(e);
		}
	}

	private void sendParticipantNames(Transaction transaction)
			throws IOException {
		Room room = this.roomManager.getRoom(roomName);

		JsonArray existingParticipantsAnnouncement = new JsonArray();
		for (Participant participant : room.getParticipants()) {
			if (!participant.getName().equals(this.name)) {
				JsonElement participantName = new JsonPrimitive(
						participant.getName());
				existingParticipantsAnnouncement.add(participantName);
			}
		}

		log.debug("PARTICIPANT {}: sending a list of {} participants",
				this.name, existingParticipantsAnnouncement.size());
		transaction.sendResponse(existingParticipantsAnnouncement);
	}

}
