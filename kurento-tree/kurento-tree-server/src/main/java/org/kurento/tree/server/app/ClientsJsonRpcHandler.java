package org.kurento.tree.server.app;

import static org.kurento.tree.client.internal.ProtocolElements.ANSWER_SDP;
import static org.kurento.tree.client.internal.ProtocolElements.OFFER_SDP;
import static org.kurento.tree.client.internal.ProtocolElements.SINK_ID;
import static org.kurento.tree.client.internal.ProtocolElements.TREE_ID;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.JsonRpcErrorException;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.client.TreeException;
import org.kurento.tree.server.treemanager.TreeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ClientsJsonRpcHandler extends DefaultJsonRpcHandler<JsonObject> {

	private static final Logger log = LoggerFactory
			.getLogger(ClientsJsonRpcHandler.class);

	private TreeManager treeManager;

	public ClientsJsonRpcHandler(TreeManager treeManager) {
		this.treeManager = treeManager;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handleRequest(Transaction transaction,
			Request<JsonObject> request) throws Exception {

		Response<JsonElement> response = null;

		try {

			Method method = this.getClass().getMethod(request.getMethod(),
					Request.class);

			response = (Response<JsonElement>) method.invoke(this, request);

			if (response != null) {
				response.setId(request.getId());
				transaction.sendResponseObject(response);
			} else {
				transaction.sendVoidResponse();
			}

		} catch (InvocationTargetException e) {
			log.error("Exception executing request " + request, e);

			transaction.sendError(e.getCause());

		} catch (NoSuchMethodException e) {
			log.error("Requesting unrecognized method '" + request.getMethod()
					+ "'");
			transaction.sendError(1,
					"Unrecognized method '" + request.getMethod() + "'", null);

		} catch (Exception e) {
			log.error("Exception processing request " + request, e);
			transaction.sendError(e);
		}
	}

	public Response<JsonElement> createTree(Request<JsonObject> request)
			throws TreeException {

		String treeId = getParam(request, TREE_ID, String.class, true);
		try {
			if (treeId == null) {
				String newTreeId = treeManager.createTree();
				return new Response<JsonElement>(null, new JsonPrimitive(
						newTreeId));
			} else {
				treeManager.createTree(treeId);
				return null;
			}
		} catch (TreeException e) {
			e.printStackTrace();
			throw new JsonRpcErrorException(2, e.getMessage());
		}
	}

	public void releaseTree(Request<JsonObject> request) {
		try {
			treeManager.releaseTree(getParam(request, TREE_ID, String.class));
		} catch (TreeException e) {
			throw new JsonRpcErrorException(2, e.getMessage());
		}
	}

	public Response<JsonElement> setTreeSource(Request<JsonObject> request) {
		try {
			String sdp = treeManager.setTreeSource(
					getParam(request, TREE_ID, String.class),
					getParam(request, OFFER_SDP, String.class));

			JsonObject result = new JsonObject();
			result.addProperty(ANSWER_SDP, sdp);

			return new Response<JsonElement>(null, result);

		} catch (TreeException e) {
			throw new JsonRpcErrorException(2, e.getMessage());
		}
	}

	public Response<JsonElement> addTreeSink(Request<JsonObject> request) {
		try {
			TreeEndpoint endpoint = treeManager.addTreeSink(
					getParam(request, TREE_ID, String.class),
					getParam(request, OFFER_SDP, String.class));

			JsonObject result = new JsonObject();
			result.addProperty(SINK_ID, endpoint.getId());
			result.addProperty(ANSWER_SDP, endpoint.getSdp());

			return new Response<JsonElement>(null, result);

		} catch (TreeException e) {
			throw new JsonRpcErrorException(2, e.getMessage());
		}
	}

	public void removeTreeSource(Request<JsonObject> request) {
		try {
			treeManager.removeTreeSource(getParam(request, TREE_ID,
					String.class));

		} catch (TreeException e) {
			throw new JsonRpcErrorException(2, e.getMessage());
		}
	}

	public void removeTreeSink(Request<JsonObject> request) {
		try {
			treeManager.removeTreeSink(
					getParam(request, TREE_ID, String.class),
					getParam(request, SINK_ID, String.class));

		} catch (TreeException e) {
			throw new JsonRpcErrorException(2, e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getParam(Request<JsonObject> request, String paramName,
			Class<T> type) {
		return getParam(request, paramName, type, false);
	}

	@SuppressWarnings("unchecked")
	public <T> T getParam(Request<JsonObject> request, String paramName,
			Class<T> type, boolean allowNull) {

		JsonObject params = request.getParams();
		if (params == null) {
			if (!allowNull) {
				throw new JsonRpcErrorException(1,
						"Invalid request lacking parameter '" + paramName + "'");
			} else {
				return null;
			}
		}

		JsonElement paramValue = params.get(paramName);
		if (paramValue == null) {
			if (allowNull) {
				return null;
			} else {
				throw new JsonRpcErrorException(1,
						"Invalid request lacking parameter '" + paramName + "'");
			}
		}

		if (type == String.class) {
			if (paramValue.isJsonPrimitive()) {
				return (T) paramValue.getAsString();
			}
		}

		throw new JsonRpcErrorException(2, "Param '" + paramName
				+ " with value '" + paramValue + "' is not a String");
	}
}
