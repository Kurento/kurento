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
import com.kurento.kmf.media.MediaPipelineFactory;

public class PhoneHandler extends DefaultJsonRpcHandler<JsonObject> {

	private final Logger log = LoggerFactory.getLogger(PhoneHandler.class);

	@Autowired
	private MediaPipelineFactory mpf;

	@Autowired
	private Registry registry;

	private Session session;

	private String name;

	private Call call;

	private Session getSession() {
		return session;
	}

	@Override
	public void handleRequest(Transaction transaction,
			Request<JsonObject> request) throws Exception {

		JsonObject params = request.getParams();

		switch (request.getMethod()) {
		case "register":
			register(transaction, params);
			break;
		case "call":
			call(transaction, params);
			break;
		default:
			break;
		}
	}

	@Override
	public void afterConnectionEstablished(Session session) throws Exception {
		this.session = session;
	}

	private void call(Transaction transaction, JsonObject params)
			throws IOException {

		String to = params.get("callTo").getAsString();
		Session toSession = registry.get(to).getSession();

		JsonObject icParams = new JsonObject();
		params.addProperty("from", name);
		JsonObject icResponse = (JsonObject) toSession.sendRequest(
				"incommingCall", icParams);

		String callResponse = icResponse.get("callResponse").getAsString();

		if ("Accept".equals(callResponse)) {

			log.info("Accepted call from '{}' to '{}'", name, to);

			call = new Call(mpf);
			call.setOutgoingPeer(name, session);
			call.setIncommingPeer(to, toSession);

			String ipSdpOffer = icResponse.get("sdpOffer").getAsString();

			log.info("SdpOffer: {}", ipSdpOffer);

			String ipSdpAnswer = call.getWebRtcForIncommingPeer().processOffer(
					ipSdpOffer);

			JsonObject scParams = new JsonObject();
			scParams.addProperty("sdpAnswer", ipSdpAnswer);

			log.info("SdpAnswer: {}", ipSdpAnswer);

			// TODO Should we expect something from client?
			toSession.sendRequest("startCommunication", scParams);

			String opSdpOffer = params.getAsJsonPrimitive("sdpOffer")
					.getAsString();

			String opSdpAnswer = call.getWebRtcForOutgoingPeer().processOffer(
					opSdpOffer);

			JsonObject response = new JsonObject();
			response.addProperty("callResponse", "Accepted");
			response.addProperty("sdpAnswer", opSdpAnswer);
			transaction.sendResponse(response);

		} else {

			JsonObject response = new JsonObject();
			response.addProperty("callResponse", "rejected");
			transaction.sendResponse(response);
		}
	}

	private void register(Transaction transaction, JsonObject params)
			throws IOException {
		this.name = params.getAsJsonPrimitive("name").getAsString();
		registry.register(name, this);
		transaction.sendResponse("registered");
	}
}
