package org.kurento.control.server;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient.RequestAndResponseType;
import org.kurento.client.internal.transport.jsonrpc.RomJsonRpcConstants;
import org.kurento.jsonrpc.Props;
import org.kurento.jsonrpc.message.Response;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class TransactionManagerTest {

	@Test
	public void test() {

		TransactionManager txManager = new TransactionManager();
		RomClientJsonRpcClient rom = new RomClientJsonRpcClient(null);

		String mediaPipelineRef = "MPR";

		RequestAndResponseType createMediaPipelineRequest = rom
				.createCreateRequest("MediaPipeline", null, false);
		txManager.updateRequest(createMediaPipelineRequest.request);

		Response<JsonElement> response = new Response<JsonElement>(null,
				new JsonPrimitive(mediaPipelineRef));
		txManager.updateResponse(response);

		RequestAndResponseType createMediaElementRequest = rom
				.createCreateRequest("PlayerEndpoint", new Props(
						"mediaPipeline", "newref:0").add("uri", "http://xxxxx"), false);
		txManager.updateRequest(createMediaElementRequest.request);

		assertThat(
				createMediaElementRequest.request.getParams()
						.get(RomJsonRpcConstants.CREATE_CONSTRUCTOR_PARAMS)
						.getAsJsonObject().get("mediaPipeline").getAsString(),
				is(mediaPipelineRef));
	}

}
