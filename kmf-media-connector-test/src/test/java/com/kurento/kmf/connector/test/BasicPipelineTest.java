package com.kurento.kmf.connector.test;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kurento.kmf.connector.test.base.BootBaseTest;
import com.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.JsonUtils;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClientWebSocket;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;

public class BasicPipelineTest extends BootBaseTest {

	private static final Logger log = LoggerFactory
			.getLogger(BasicPipelineTest.class);

	private JsonRpcClient client;
	
	@Before
	public void setup() throws IOException {
		
		HttpHeaders headers = new HttpHeaders();
		headers.add(
				"X-Auth-Token",
				"jm1-vF_kcarImdhRh0v4axk0FcndHbZPaNRpiRMyddp2Qb1Kojllfm63Ikv3uN3KFx850CCYzUamjNl5GwApnQ");
		
		client = new JsonRpcClientWebSocket("ws://localhost:" + getPort()

				+ "/thrift", headers);
		

		client.setServerRequestHandler(new DefaultJsonRpcHandler<JsonObject>() {
			@Override
			public void handleRequest(Transaction transaction,
					Request<JsonObject> request) throws Exception {

				log.info("Request received: " + request);
			}
		});

		log.info("Client started");
	}

	@After
	public void teardown() throws IOException {
		client.close();
		log.info("Client finished");
	}

	private JsonObject sendRequest(String request) throws IOException {

		JsonObject requestJson = createJsonObject(request);

		JsonElement paramsProp = requestJson.get("params");
		JsonObject params = null;
		if (paramsProp != null) {
			params = paramsProp.getAsJsonObject();
		}

		return client.sendRequest(requestJson.get("method").getAsString(),
				params, JsonObject.class);
	}

	private JsonObject createJsonObject(String request) {
		return JsonUtils.fromJson(request, JsonObject.class);
	}

	@Test
	public void test() throws IOException {
		
		JsonObject pipelineCreation = sendRequest("{\n" +
				"      \"jsonrpc\": \"2.0\",\n" +
				"      \"method\": \"create\",\n" + 
				"      \"params\": {\n" + 
				"        \"type\": \"MediaPipeline\"\n" + 
				"      },\n" + 
				"      \"id\": 1\n" + 
				"    }");
		
		String pipelineId = pipelineCreation.get("value").getAsString();
		String sessionId = "XXX";

		JsonObject playerEndpointCreation = sendRequest("{\n" +
				"      \"jsonrpc\": \"2.0\",\n" +
				"      \"method\": \"create\",\n" + 
				"      \"params\": {\n" + 
				"        \"type\": \"PlayerEndpoint\",\n" + 
				"        \"constructorParams\": {\n" + 
				"          \"mediaPipeline\": \""+pipelineId+"\",\n" + 
				"          \"uri\": \"http://localhost:8000/video.avi\"\n" + 
				"        },\n" + 
				"        \"sessionId\": \""+sessionId+"\"\n" + 
				"      },\n" + 
				"      \"id\": 2\n" + 
				"    }");
		
		String playerId = playerEndpointCreation.get("value").getAsString();

		JsonObject httpPlayerEndpointCreation = sendRequest("{\n" +
				"      \"jsonrpc\": \"2.0\",\n" +
				"      \"method\": \"create\",\n" + 
				"      \"params\": {\n" + 
				"        \"type\": \"HttpGetEndpoint\",\n" + 
				"        \"constructorParams\": {\n" + 
				"          \"mediaPipeline\": \""+pipelineId+"\"\n" + 
				"        },\n" + 
				"        \"sessionId\": \""+sessionId+"\"\n" + 
				"      },\n" + 
				"      \"id\": 3\n" + 
				"    }");
		
		String httpGetId = httpPlayerEndpointCreation.get("value").getAsString();

		sendRequest(" {\n" + 
				"      \"jsonrpc\": \"2.0\",\n" +
				"      \"method\": \"invoke\",\n" + 
				"      \"params\": {\n" + 
				"        \"object\": \""+playerId+"\",\n" + 
				"        \"operation\": \"connect\",\n" + 
				"        \"operationParams\": {\n" + 
				"          \"sink\": \""+httpGetId+"\"\n" + 
				"        },\n" + 
				"        \"sessionId\": \""+sessionId+"\"\n" + 
				"      },\n" + 
				"      \"id\": 4\n" + 
				"    }");
		
		JsonObject getUrlResponse = sendRequest(" {\n" +
				"      \"jsonrpc\": \"2.0\",\n" +
				"      \"method\": \"invoke\",\n" + 
				"      \"params\": {\n" + 
				"        \"object\": \""+httpGetId+"\",\n" + 
				"        \"operation\": \"getUrl\",\n" + 
				"        \"sessionId\": \""+sessionId+"\"\n" + 
				"      },\n" + 
				"      \"id\": 5\n" + 
				"    }");
		
		sendRequest(" {\n" + 
				"      \"jsonrpc\": \"2.0\",\n" +
				"      \"method\": \"subscribe\",\n" + 
				"      \"params\": {\n" + 
				"        \"type\": \"Error\",\n" +
				"        \"object\": \""+playerId+"\",\n" + 
				"        \"ip\": \"127.0.0.1\",\n" +
				"        \"port\": 9999,\n" +
				"        \"sessionId\": \""+sessionId+"\"\n" + 
				"      },\n" + 
				"      \"id\": 7\n" + 
				"    }");
		
		String url = getUrlResponse.get("value").getAsString();		
		
		sendRequest(" {\n" + 
				"      \"jsonrpc\": \"2.0\",\n" +
				"      \"method\": \"invoke\",\n" + 
				"      \"params\": {\n" + 
				"        \"object\": \""+playerId+"\",\n" + 
				"        \"operation\": \"play\",\n" + 
				"        \"sessionId\": \""+sessionId+"\"\n" + 
				"      },\n" + 
				"      \"id\": 8\n" + 
				"    }");
		
		Assert.assertTrue(url.contains("9091"));		
		
		sendRequest(" {\n" + 
				"      \"jsonrpc\": \"2.0\",\n" +
				"      \"method\": \"release\",\n" + 
				"      \"params\": {\n" + 
				"        \"object\": \""+pipelineId+"\"\n" + 
				"      },\n" + 
				"      \"id\": 9\n" + 
				"    }");
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
