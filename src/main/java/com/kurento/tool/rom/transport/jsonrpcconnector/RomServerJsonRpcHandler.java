package com.kurento.tool.rom.transport.jsonrpcconnector;

import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.CREATE_CONSTRUCTOR_PARAMS;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.CREATE_METHOD;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.CREATE_TYPE;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.INVOKE_METHOD;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.INVOKE_OBJECT;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.INVOKE_OPERATION_NAME;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.INVOKE_OPERATION_PARAMS;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.RELEASE_METHOD;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.RELEASE_OBJECT;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.JsonUtils;
import com.kurento.kmf.jsonrpcconnector.Props;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.tool.rom.server.RomException;
import com.kurento.tool.rom.server.RomServer;

public class RomServerJsonRpcHandler extends DefaultJsonRpcHandler<JsonObject> {

	private static Logger LOG = LoggerFactory
			.getLogger(RomServerJsonRpcHandler.class);

	private RomServer server;

	public RomServerJsonRpcHandler(String packageName, String classSuffix) {
		server = new RomServer(packageName, classSuffix);
	}

	@Override
	public void handleRequest(Transaction transaction,
			Request<JsonObject> request) throws Exception {

		try {

			JsonObject params = request.getParams();
			String method = request.getMethod();
			if (method.equals(INVOKE_METHOD)) {

				String objectRef = getAsString(params, INVOKE_OBJECT,
						"object reference");

				String operationName = getAsString(params,
						INVOKE_OPERATION_NAME, "method to be invoked");

				JsonObject operationParams = params
						.getAsJsonObject(INVOKE_OPERATION_PARAMS);

				handleInvokeCommand(transaction, objectRef, operationName,
						operationParams);

			} else if (method.equals(RELEASE_METHOD)) {

				String objectRef = getAsString(params, RELEASE_OBJECT,
						"object reference to be released");

				handleReleaseCommand(transaction, objectRef);

			} else if (method.equals(CREATE_METHOD)) {

				String type = getAsString(params, CREATE_TYPE,
						"RemoteClass of the object to be created");

				handleCreateCommand(transaction, type,
						params.getAsJsonObject(CREATE_CONSTRUCTOR_PARAMS));
			}

		} catch (RomException e) {
			try {
				transaction.sendError(e);
			} catch (IOException e1) {
				LOG.warn("Exception while sending a response", e);
			}
		} catch (IOException e) {
			LOG.warn("Exception while sending a response", e);
		}
	}

	private String getAsString(JsonObject jsonObject, String propName,
			String propertyDescription) {

		if (jsonObject == null) {
			throw new RomException("There are no params in the request");
		}

		JsonElement element = jsonObject.get(propName);
		if (element == null) {
			throw new RomException("It is necessary a property '" + propName
					+ "' with " + propertyDescription);
		} else {
			return (String) element.getAsString();
		}
	}

	private void handleCreateCommand(Transaction transaction, String type,
			JsonObject constructorParams) throws IOException {

		Object result = server.create(type,
				JsonUtils.fromJson(constructorParams, Props.class));

		transaction.sendResponse(result);
	}

	private void handleReleaseCommand(Transaction transaction, String objectRef) {
		server.release(objectRef);
	}

	private void handleInvokeCommand(Transaction transaction, String objectRef,
			String operationName, JsonObject operationParams)
			throws IOException {

		Object result = server.invoke(objectRef, operationName,
				JsonUtils.fromJson(operationParams, Props.class), Object.class);

		transaction.sendResponse(result);
	}

}