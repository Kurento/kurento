package com.kurento.kmf.phone;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonObject;
import com.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.Session;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;

public class ParticipantHandler extends DefaultJsonRpcHandler<JsonObject> {

	private static final Logger logger = LoggerFactory
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
			break;
		case "sendVideo":
			sendVideoToParticipant(transaction, params);
			break;
		default:
			break;
		}
	}

	@Override
	public void afterConnectionEstablished(Session session) throws Exception {
		this.session = session;
	}

	private void joinRoom(Transaction transaction, JsonObject params)
			throws IOException {

		this.roomName = params.get("room").getAsString();
		this.name = params.get("name").getAsString();

		Room room;
		logInfo("trying to join room {}", this.roomName);
		try {
			room = roomManager.getRoom(this.roomName);
			this.myself = room.join(this.name, session);
		} catch (IllegalArgumentException e) {
			// TODO don't send only the exception, but add more info
			transaction.sendError(e);
		}
	}

	/**
	 * @param name
	 * @throws IOException
	 */
	private void sendVideoToParticipant(Transaction transaction,
			JsonObject params) throws IOException {

		String to = params.get("recipient").getAsString();
		Participant recipient = roomManager.getRoom(this.roomName)
				.getParticipant(to);

		logInfo("connecting with {} in room {}", to, this.roomName);
		String ipSdpOffer = params.get("sdpOffer").getAsString();

		logInfo("SdpOffer is {}", ipSdpOffer);

		String ipSdpAnswer = recipient.receiveVideoFrom(myself)
				.processOffer(ipSdpOffer);

		JsonObject scParams = new JsonObject();
		scParams.addProperty("sdpAnswer", ipSdpAnswer);

		logInfo("SdpAnswer is {}", ipSdpAnswer);
		transaction.sendResponse(scParams);
	}

	private void logInfo(String message, Object... params) {
		logger.info("PARTICIPANT {}: ", this.name, params);
	}

}
