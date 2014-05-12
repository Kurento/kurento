package kmf.broker.server;

import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants;

public class ObjectIdsConverter {

	public void convertRefsFromBrokerToReal(String method, JsonObject params,
			Map<String, MediaPipelineInfo> pipelinesByBroker) {

		// TODO: Implement a reliable way to convert all realObjectIds in
		// parameters to brokerObjectIds. To do this it is necessary to know the
		// API. The current implementation is fragile because it manages only
		// the
		// common cases.

		if (RomJsonRpcConstants.CREATE_METHOD.equals(method)) {

			JsonObject paramsJson = (JsonObject) params
					.get(RomJsonRpcConstants.CREATE_CONSTRUCTOR_PARAMS);
			if (paramsJson.has("mediaPipeline")) {

				String brokerPipelineId = paramsJson.get("mediaPipeline")
						.getAsString();
				String realPipelineId = pipelinesByBroker.get(brokerPipelineId)
						.getRealPipelineId();
				paramsJson.addProperty("mediaPipeline", realPipelineId);

			} else if (paramsJson.has("hub")) {

				String brokerObjectId = paramsJson.get("hub").getAsString();
				String realObjectId = extractBrokerPipelineFromBrokerObjectId(brokerObjectId);
				paramsJson.addProperty("hub", realObjectId);
			}

		} else if (params.has(RomJsonRpcConstants.INVOKE_OBJECT)) {

			String brokerObjectId = params.get(
					RomJsonRpcConstants.INVOKE_OBJECT).getAsString();

			String realObjectId = extractRealObjectIdFromBrokerObjectId(
					brokerObjectId, pipelinesByBroker);

			params.addProperty(RomJsonRpcConstants.INVOKE_OBJECT, realObjectId);

			if (params.has(RomJsonRpcConstants.INVOKE_OPERATION_PARAMS)) {

				JsonObject paramsJson = (JsonObject) params
						.get(RomJsonRpcConstants.INVOKE_OPERATION_PARAMS);

				changeParamFromBrokerToReal(paramsJson, pipelinesByBroker);
			}
		}
	}

	private JsonElement changeParamFromBrokerToReal(JsonElement value,
			Map<String, MediaPipelineInfo> pipelinesByBroker) {

		if (value instanceof JsonPrimitive) {
			String stringValue = ((JsonPrimitive) value).getAsString();
			if (stringValue.startsWith("amq.gen-")) {

				return new JsonPrimitive(extractRealObjectIdFromBrokerObjectId(
						stringValue, pipelinesByBroker));
			}

		} else if (value instanceof JsonObject) {

			JsonObject jsonObjectValue = (JsonObject) value;

			for (Entry<String, JsonElement> entry : jsonObjectValue.entrySet()) {

				JsonElement element = changeParamFromBrokerToReal(
						entry.getValue(), pipelinesByBroker);
				if (element != null) {
					jsonObjectValue.add(entry.getKey(), element);
				}
			}

		} else if (value instanceof JsonArray) {

			JsonArray array = (JsonArray) value;
			JsonArray newArray = new JsonArray();
			for (int i = 0; i < array.size(); i++) {

				JsonElement element = changeParamFromBrokerToReal(array.get(i),
						pipelinesByBroker);
				if (element != null) {
					newArray.add(element);
				} else {
					newArray.add(array.get(i));
				}
			}

			return newArray;
		}

		return null;
	}

	public void convertRefsFromRealToBroker(String method, JsonElement params) {
		// TODO Auto-generated method stub

	}

	public String extractBrokerPipelineFromBrokerObjectId(String brokerObjectId) {
		int slashIndex = brokerObjectId.indexOf('/');
		if (slashIndex == -1) {
			// It is a BrokerPipelineId
			return brokerObjectId;
		} else {
			// It is another object
			return brokerObjectId.substring(0, slashIndex);
		}
	}

	public String extractRealObjectIdFromBrokerObjectId(String brokerObjectId,
			Map<String, MediaPipelineInfo> pipelinesByBrokerId) {

		int slashIndex = brokerObjectId.indexOf('/');
		if (slashIndex == -1) {
			// It is a BrokerPipelineId
			return pipelinesByBrokerId.get(brokerObjectId).getRealPipelineId();
		} else {
			// It is another object
			return brokerObjectId.substring(slashIndex + 1,
					brokerObjectId.length());
		}
	}

	public String createBrokerObjectId(String brokerPipelineId,
			String realObjectId) {
		return brokerPipelineId + "/" + realObjectId;
	}
}
