package org.kurento.kmf.connector.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.kurento.kmf.connector.test.base.BootBaseTest;
import org.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import org.kurento.kmf.jsonrpcconnector.JsonUtils;
import org.kurento.kmf.jsonrpcconnector.Transaction;
import org.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import org.kurento.kmf.jsonrpcconnector.client.JsonRpcClientWebSocket;
import org.kurento.kmf.jsonrpcconnector.internal.message.Request;

public class MultipleBasicPipelineTest extends BootBaseTest {

	private static final Logger LOG = LoggerFactory
			.getLogger(MultipleBasicPipelineTest.class);
	
	private static final int CONCURRENT_PIPELINES = 5;
	private static final int SEQUENTIAL_PIPELINES = 5;

	public JsonRpcClient createClient() throws IOException {

		HttpHeaders headers = new HttpHeaders();
		headers.add(
				"X-Auth-Token",
				"jm1-vF_kcarImdhRh0v4axk0FcndHbZPaNRpiRMyddp2Qb1Kojllfm63Ikv3uN3KFx850CCYzUamjNl5GwApnQ");

		JsonRpcClient client = new JsonRpcClientWebSocket("ws://localhost:"
				+ getPort()	+ "/thrift", headers);

		client.setServerRequestHandler(new DefaultJsonRpcHandler<JsonObject>() {
			@Override
			public void handleRequest(Transaction transaction,
					Request<JsonObject> request) throws Exception {

				LOG.info("Request received: " + request);
			}
		});

		LOG.info("Client started");

		return client;
	}

	public void teardown(JsonRpcClient client) throws IOException {
		client.close();
		LOG.info("Client finished");
	}

	private JsonObject sendRequest(JsonRpcClient client, String request)
			throws IOException {

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
	public void concurrentPipelines() throws IOException, InterruptedException,
			ExecutionException {

		ExecutorService execService = Executors
				.newFixedThreadPool(CONCURRENT_PIPELINES);

		List<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
		for (int i = 0; i < SEQUENTIAL_PIPELINES; i++) {
			execService.submit(new Callable<Boolean>() {

				public Boolean call() {
					try {
						long waitTime = (long)(Math.random()*1000);
						
						LOG.info("Client waiting "+waitTime+" millis");
						
						Thread.sleep(waitTime);
						return createPipeline();
					} catch (Exception e) {
						e.printStackTrace();
						return false;
					} 
				}
			});
		}

		for (Future<Boolean> result : results) {
			Assert.assertTrue(result.get());
		}

		execService.shutdown();
		execService.awaitTermination(10, TimeUnit.SECONDS);
	}
	
	@Test
	public void sequentialPipelinesTest() throws IOException, InterruptedException, ExecutionException{
		Assert.assertTrue(sequentialPipelines());
	}
	
	public boolean sequentialPipelines() throws IOException, InterruptedException,
			ExecutionException {
		
		for(int i=0; i<SEQUENTIAL_PIPELINES; i++){
			LOG.info("Starting pipeline {}",i);
			if(!createPipeline()){
				return false;
			}	
			LOG.info("Finished pipeline {}",i);
		}		
		return true;
	}
	
	@Test
	public void secuentialPipelinesOneConnectionTest() throws IOException, InterruptedException, ExecutionException{
		Assert.assertTrue(secuentialPipelinesOneConnection());
	}
	
	public boolean secuentialPipelinesOneConnection() throws IOException, InterruptedException,
			ExecutionException {
		
		JsonRpcClient client = createClient();
		
		for(int i=0; i<SEQUENTIAL_PIPELINES; i++){
			LOG.info("Starting pipeline OneConnection {}",i);
			if(!createPipeline(client)){
				return false;
			}
			LOG.info("Finished pipeline OneConnection {}",i);
		}		
		
		client.close();
		return true;
	}
	
	@Test
	public void concurrentSequentialPipelines() throws IOException, InterruptedException,
			ExecutionException {

		ExecutorService execService = Executors
				.newFixedThreadPool(CONCURRENT_PIPELINES);

		List<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
		for (int i = 0; i < SEQUENTIAL_PIPELINES; i++) {
			execService.submit(new Callable<Boolean>() {

				public Boolean call() {
					try {
						long waitTime = (long)(Math.random()*1000);
						
						LOG.info("Client waiting "+waitTime+" millis");
						
						Thread.sleep(waitTime);
						return sequentialPipelines();
					} catch (Exception e) {
						e.printStackTrace();
						return false;
					} 
				}
			});
		}

		for (Future<Boolean> result : results) {
			Assert.assertTrue(result.get());
		}

		execService.shutdown();
		execService.awaitTermination(10, TimeUnit.SECONDS);
	}

	private boolean createPipeline() throws IOException {

		JsonRpcClient client = createClient();

		boolean result = createPipeline(client);
		
		client.close();
				
		return result;
	}
	

	private boolean createPipeline(JsonRpcClient client) throws IOException {
		JsonObject pipelineCreation = sendRequest(client, "{\n"
				+ "      \"jsonrpc\": \"2.0\",\n"
				+ "      \"method\": \"create\",\n" + "      \"params\": {\n"
				+ "        \"type\": \"MediaPipeline\"\n" + "      },\n"
				+ "      \"id\": 1\n" + "    }");

		String pipelineId = pipelineCreation.get("value").getAsString();
		String sessionId = "XXX";

		JsonObject playerEndpointCreation = sendRequest(client, "{\n"
				+ "      \"jsonrpc\": \"2.0\",\n"
				+ "      \"method\": \"create\",\n" + "      \"params\": {\n"
				+ "        \"type\": \"PlayerEndpoint\",\n"
				+ "        \"constructorParams\": {\n"
				+ "          \"mediaPipeline\": \"" + pipelineId + "\",\n"
				+ "          \"uri\": \"http://localhost:8000/video.avi\"\n"
				+ "        },\n" + "        \"sessionId\": \"" + sessionId
				+ "\"\n" + "      },\n" + "      \"id\": 2\n" + "    }");

		String playerId = playerEndpointCreation.get("value").getAsString();

		JsonObject httpPlayerEndpointCreation = sendRequest(client, "{\n"
				+ "      \"jsonrpc\": \"2.0\",\n"
				+ "      \"method\": \"create\",\n" + "      \"params\": {\n"
				+ "        \"type\": \"HttpGetEndpoint\",\n"
				+ "        \"constructorParams\": {\n"
				+ "          \"mediaPipeline\": \"" + pipelineId + "\"\n"
				+ "        },\n" + "        \"sessionId\": \"" + sessionId
				+ "\"\n" + "      },\n" + "      \"id\": 3\n" + "    }");

		String httpGetId = httpPlayerEndpointCreation.get("value")
				.getAsString();

		sendRequest(client, " {\n" + "      \"jsonrpc\": \"2.0\",\n"
				+ "      \"method\": \"invoke\",\n" + "      \"params\": {\n"
				+ "        \"object\": \"" + playerId + "\",\n"
				+ "        \"operation\": \"connect\",\n"
				+ "        \"operationParams\": {\n" + "          \"sink\": \""
				+ httpGetId + "\"\n" + "        },\n"
				+ "        \"sessionId\": \"" + sessionId + "\"\n"
				+ "      },\n" + "      \"id\": 4\n" + "    }");

		JsonObject getUrlResponse = sendRequest(client, " {\n"
				+ "      \"jsonrpc\": \"2.0\",\n"
				+ "      \"method\": \"invoke\",\n" + "      \"params\": {\n"
				+ "        \"object\": \"" + httpGetId + "\",\n"
				+ "        \"operation\": \"getUrl\",\n"
				+ "        \"sessionId\": \"" + sessionId + "\"\n"
				+ "      },\n" + "      \"id\": 5\n" + "    }");

		sendRequest(client, " {\n" + "      \"jsonrpc\": \"2.0\",\n"
				+ "      \"method\": \"subscribe\",\n"
				+ "      \"params\": {\n" + "        \"type\": \"Error\",\n"
				+ "        \"object\": \"" + playerId + "\",\n"
				+ "        \"ip\": \"127.0.0.1\",\n"
				+ "        \"port\": 9999,\n" + "        \"sessionId\": \""
				+ sessionId + "\"\n" + "      },\n" + "      \"id\": 6\n"
				+ "    }");

		String url = getUrlResponse.get("value").getAsString();

		sendRequest(client, " {\n" + "      \"jsonrpc\": \"2.0\",\n"
				+ "      \"method\": \"invoke\",\n" + "      \"params\": {\n"
				+ "        \"object\": \"" + playerId + "\",\n"
				+ "        \"operation\": \"play\",\n"
				+ "        \"sessionId\": \"" + sessionId + "\"\n"
				+ "      },\n" + "      \"id\": 7\n" + "    }");
		
		sendRequest(client," {\n" + 
				"      \"jsonrpc\": \"2.0\",\n" +
				"      \"method\": \"release\",\n" + 
				"      \"params\": {\n" + 
				"        \"object\": \""+pipelineId+"\"\n" + 
				"      },\n" + 
				"      \"id\": 8\n" + 
				"    }");
		
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return url.contains("http");
	}
}
