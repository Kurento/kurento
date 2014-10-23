package org.kurento.tree.client;

import static org.kurento.tree.protocol.ProtocolElements.ADD_TREE_SINK_METHOD;
import static org.kurento.tree.protocol.ProtocolElements.ANSWER_SDP;
import static org.kurento.tree.protocol.ProtocolElements.CREATE_TREE_METHOD;
import static org.kurento.tree.protocol.ProtocolElements.OFFER_SDP;
import static org.kurento.tree.protocol.ProtocolElements.RELEASE_TREE_METHOD;
import static org.kurento.tree.protocol.ProtocolElements.REMOVE_TREE_SINK_METHOD;
import static org.kurento.tree.protocol.ProtocolElements.REMOVE_TREE_SOURCE_METHOD;
import static org.kurento.tree.protocol.ProtocolElements.SET_TREE_SOURCE_METHOD;
import static org.kurento.tree.protocol.ProtocolElements.SINK_ID;
import static org.kurento.tree.protocol.ProtocolElements.TREE_ID;

import java.io.IOException;

import org.kurento.jsonrpc.JsonRpcErrorException;
import org.kurento.jsonrpc.JsonRpcException;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.kurento.tree.protocol.TreeEndpoint;
import org.kurento.tree.server.treemanager.TreeException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class KurentoTreeClient {

	private JsonRpcClient client;

	public KurentoTreeClient(String wsUri) {
		this(new JsonRpcClientWebSocket(wsUri));
	}

	public KurentoTreeClient(JsonRpcClient client) {
		this.client = client;
	}

	public String createTree() throws IOException {
		JsonElement response = client.sendRequest(CREATE_TREE_METHOD);
		return JsonUtils.extractJavaValueFromResult(response, String.class);
	}

	public void releaseTree(String treeId) throws TreeException, IOException {
		JsonObject params = new JsonObject();
		params.addProperty(TREE_ID, treeId);
		try {
			client.sendRequest(RELEASE_TREE_METHOD, params);
		} catch (JsonRpcErrorException e) {
			processException(e);
		}
	}

	public String setTreeSource(String treeId, String offerSdp)
			throws TreeException, IOException {

		JsonObject params = new JsonObject();
		params.addProperty(TREE_ID, treeId);
		params.addProperty(OFFER_SDP, offerSdp);

		try {

			JsonElement result = client.sendRequest(SET_TREE_SOURCE_METHOD,
					params);

			return getResponseProperty(result, ANSWER_SDP, String.class);

		} catch (JsonRpcErrorException e) {
			processException(e);
			return null;
		}
	}

	public void removeTreeSource(String treeId) throws TreeException,
			IOException {

		JsonObject params = new JsonObject();
		params.addProperty(TREE_ID, treeId);

		try {

			client.sendRequest(REMOVE_TREE_SOURCE_METHOD, params);

		} catch (JsonRpcErrorException e) {
			processException(e);
		}
	}

	public TreeEndpoint addTreeSink(String treeId, String offerSdp)
			throws IOException, TreeException {

		JsonObject params = new JsonObject();
		params.addProperty(TREE_ID, treeId);
		params.addProperty(OFFER_SDP, offerSdp);

		try {

			JsonElement result = client.sendRequest(ADD_TREE_SINK_METHOD,
					params);

			return new TreeEndpoint(getResponseProperty(result, ANSWER_SDP,
					String.class), getResponseProperty(result, SINK_ID,
					String.class));

		} catch (JsonRpcErrorException e) {
			processException(e);
			return null;
		}
	}

	public void removeTreeSink(String treeId, String sinkId)
			throws TreeException, IOException {

		JsonObject params = new JsonObject();
		params.addProperty(TREE_ID, treeId);
		params.addProperty(SINK_ID, sinkId);

		try {

			client.sendRequest(REMOVE_TREE_SINK_METHOD, params);

		} catch (JsonRpcErrorException e) {
			processException(e);
		}
	}

	private void processException(JsonRpcErrorException e) throws TreeException {
		if (e.getCode() == 2) {
			throw new TreeException(e.getMessage());
		} else {
			throw e;
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getResponseProperty(JsonElement result, String property,
			Class<T> type) {

		if (!(result instanceof JsonObject)) {
			throw new JsonRpcException(
					"Invalid response format. The response '" + result
							+ "' should be a Json object");
		}

		JsonElement paramValue = ((JsonObject) result).get(property);
		if (paramValue == null) {
			throw new JsonRpcException("Invalid response lacking property '"
					+ property + "'");
		}

		if (type == String.class) {
			if (paramValue.isJsonPrimitive()) {
				return (T) paramValue.getAsString();
			}
		}

		throw new JsonRpcException("Property '" + property + " with value '"
				+ paramValue + "' is not a String");
	}
}
