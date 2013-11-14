//package com.kurento.kmf.connector;
//
//import java.util.Arrays;
//import java.util.Collection;
//
//import javax.el.MethodNotFoundException;
//
//import org.junit.Assert;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.junit.runners.Parameterized;
//import org.junit.runners.Parameterized.Parameters;
//
//import com.google.gson.JsonElement;
//import com.google.gson.JsonObject;
//import com.kurento.kmf.jsonrpcconnector.JsonUtils;
//
//@RunWith(Parameterized.class)
//public class RequestResolutionTest {
//
//	@Parameters(name = "{0}")
//	public static Collection<Object[]> data() {
//		return Arrays
//				.asList(new Object[][] {
//
//						{
//								"release",
//								"{\n" + "  \"jsonrpc\": \"2.0\",\n"
//										+ "  \"id\": 1,\n" + "\n"
//										+ "  \"method\": \"release\",\n"
//										+ "  \"params\":\n" + "  {\n"
//										+ "    \"mediaObject\":\n" + "    {\n"
//										+ "      \"id\": 1234,\n"
//										+ "      \"token\": \"asdf\"\n"
//										+ "    }\n" + "  }\n" + "}" },
//						{
//								"subscribeEvent",
//								"{\n" + "  \"jsonrpc\": \"2.0\",\n"
//										+ "  \"id\": 1,\n" + "\n"
//										+ "  \"method\": \"subscribeEvent\",\n"
//										+ "  \"params\":\n" + "  {\n"
//										+ "    \"mediaObject\":\n" + "    {\n"
//										+ "      \"id\": 1234,\n"
//										+ "      \"token\": \"asdf\"\n"
//										+ "    },\n"
//										+ "    \"type\": \"close\"\n" + "  }\n"
//										+ "}" },
//						{
//								"unsubscribeEvent",
//								"{\n"
//										+ "  \"jsonrpc\": \"2.0\",\n"
//										+ "  \"id\": 1,\n"
//										+ "\n"
//										+ "  \"method\": \"unsubscribeEvent\",\n"
//										+ "  \"params\":\n" + "  {\n"
//										+ "    \"mediaObject\":\n" + "    {\n"
//										+ "      \"id\": 1234,\n"
//										+ "      \"token\": \"asdf\"\n"
//										+ "    },\n"
//										+ "    \"callbackToken\": \"zxcv\"\n"
//										+ "  }\n" // Changed from token to
//													// callbackToken
//										+ "}" },
//						{
//								"subscribeError",
//								"{\n" + "  \"jsonrpc\": \"2.0\",\n"
//										+ "  \"id\": 1,\n" + "\n"
//										+ "  \"method\": \"subscribeError\",\n"
//										+ "  \"params\":\n" + "  {\n"
//										+ "    \"mediaObject\":\n" + "    {\n"
//										+ "      \"id\": 1234,\n"
//										+ "      \"callbackToken\": \"asdf\"\n" // Changed
//																				// from
//																				// token
//																				// to
//																				// callbackToken
//										+ "    }\n" + "  }\n" + "}" },
//						{
//								"unsubscribeError",
//								"{\n"
//										+ "  \"jsonrpc\": \"2.0\",\n"
//										+ "  \"id\": 1,\n"
//										+ "\n"
//										+ "  \"method\": \"unsubscribeError\",\n"
//										+ "  \"params\":\n" + "  {\n"
//										+ "    \"mediaObject\":\n" + "    {\n"
//										+ "      \"id\": 1234,\n"
//										+ "      \"token\": \"asdf\"\n"
//										+ "    },\n"
//										+ "    \"callbackToken\": \"zxcv\"\n"
//										+ "  }\n" // Changed from token to
//													// callbackToken
//										+ "}" },
//						{
//								"invoke",
//								"{\n" + "  \"jsonrpc\": \"2.0\",\n"
//										+ "  \"id\": 1,\n" + "\n"
//										+ "  \"method\": \"invoke\",\n"
//										+ "  \"params\":\n" + "  {\n"
//										+ "    \"mediaObject\":\n" + "    {\n"
//										+ "      \"id\": 1234,\n"
//										+ "      \"token\": \"asdf\"\n"
//										+ "    },\n"
//										+ "    \"method\": \"zxcv\",\n"
//										+ "    \"params\": {}\n" + "  }\n"
//										+ "}\n" + "" },
//						{
//								"createMediaPipeline",
//								"{\n"
//										+ "  \"jsonrpc\": \"2.0\",\n"
//										+ "  \"id\": 1,\n"
//										+ "\n"
//										+ "  \"method\": \"createMediaPipeline\"\n"
//										+ "}" },
//						{
//								"createMediaElement1",
//								"{\n"
//										+ "  \"jsonrpc\": \"2.0\",\n"
//										+ "  \"id\": 2,\n"
//										+ "\n"
//										+ "  \"method\": \"createMediaElement\",\n"
//										+ "  \"params\":\n" + "  {\n"
//										+ "    \"pipeline\":\n" + "    {\n"
//										+ "      \"id\": 1234,\n"
//										+ "      \"token\": \"asdf\"\n"
//										+ "    },\n"
//										+ "    \"type\": \"JackVaderFilter\"\n"
//										+ "  }\n" + "}" },
//						{
//								"createMediaElement2",
//								"{\"jsonrpc\":\"2.0\",\"method\":\"createMediaElement\",\"params\":{\"type\":\"PlayerEndPoint\",\"params\":{\"uri\":\"http://localhost:8000/video.avi\"},\"pipeline\":{\"id\":1276985117,\"token\":\"f478cf7b-2795-4031-8a86-ddeb8943752f\"}},\"id\":1}" },
//						{
//								"createMediaElement3",
//								"{\"jsonrpc\":\"2.0\",\"method\":\"createMediaElement\",\"params\":{\"type\":\"PlayerEndPoint\",\"params\":{},\"pipeline\":{\"id\":1276985117,\"token\":\"f478cf7b-2795-4031-8a86-ddeb8943752f\"}},\"id\":1}" },
//
//						{
//								"createMediaMixer",
//								"{\n"
//										+ "  \"jsonrpc\": \"2.0\",\n"
//										+ "  \"id\": 2,\n"
//										+ "\n"
//										+ "  \"method\": \"createMediaMixer\",\n"
//										+ "  \"params\":\n" + "  {\n"
//										+ "    \"pipeline\":\n" + "    {\n"
//										+ "      \"id\": 1234,\n"
//										+ "      \"token\": \"asdf\"\n"
//										+ "    },\n"
//										+ "    \"type\": \"mixer\"\n" + "  }\n"
//										+ "}" },
//						{
//								"connect1",
//								"{\n"
//										+ "  \"jsonrpc\": \"2.0\",\n"
//										+ "  \"id\": 2,\n"
//										+ "\n"
//										+ "  \"method\": \"connectElements\",\n"
//										+ "  \"params\":\n" + "  {\n"
//										+ "    \"source\":\n" + "    {\n"
//										+ "      \"id\": 1234,\n"
//										+ "      \"token\": \"asdf\"\n"
//										+ "    },\n" + "    \"sink\":\n"
//										+ "    {\n" + "      \"id\": 5678,\n"
//										+ "      \"token\": \"qwer\"\n"
//										+ "    }\n" + "  }\n" + "}" },
//						{
//								"connect2",
//								"{\n"
//										+ "  \"jsonrpc\": \"2.0\",\n"
//										+ "  \"id\": 2,\n"
//										+ "\n"
//										+ "  \"method\": \"connectElements\",\n"
//										+ "  \"params\":\n" + "  {\n"
//										+ "    \"source\":\n" + "    {\n"
//										+ "      \"id\": 1234,\n"
//										+ "      \"token\": \"asdf\"\n"
//										+ "    },\n" + "    \"sink\":\n"
//										+ "    {\n" + "      \"id\": 5678,\n"
//										+ "      \"token\": \"qwer\"\n"
//										+ "    },\n"
//										+ "    \"type\": \"mixer\"\n" + "  }\n"
//										+ "}" },
//						{
//								"connect3",
//								"{\n"
//										+ "  \"jsonrpc\": \"2.0\",\n"
//										+ "  \"id\": 2,\n"
//										+ "\n"
//										+ "  \"method\": \"connectElements\",\n"
//										+ "  \"params\":\n"
//										+ "  {\n"
//										+ "    \"source\":\n"
//										+ "    {\n"
//										+ "      \"id\": 1234,\n"
//										+ "      \"token\": \"asdf\"\n"
//										+ "    },\n"
//										+ "    \"sink\":\n"
//										+ "    {\n"
//										+ "      \"id\": 5678,\n"
//										+ "      \"token\": \"qwer\"\n"
//										+ "    },\n"
//										+ "    \"type\": \"mixer\",\n"
//										+ "    \"mediaDescription\": \"asdf jkl\"\n" // Changed
//																						// description
//																						// for
//																						// mediaDescription
//										+ "  }\n" + "}" },
//						{
//								"connect4",
//								"{\n" + "  \"jsonrpc\": \"2.0\",\n"
//										+ "  \"id\": 2,\n" + "\n"
//										+ "  \"method\": \"connect\",\n"
//										+ "  \"params\":\n" + "  {\n"
//										+ "    \"source\":\n" + "    {\n"
//										+ "      \"id\": 1234,\n"
//										+ "      \"token\": \"asdf\"\n"
//										+ "    },\n" + "    \"sink\":\n"
//										+ "    {\n" + "      \"id\": 5678,\n"
//										+ "      \"token\": \"qwer\"\n"
//										+ "    }\n" + "  }\n" + "}" },
//						{
//								"disconnect",
//								"{\n" + "  \"jsonrpc\": \"2.0\",\n"
//										+ "  \"id\": 2,\n" + "\n"
//										+ "  \"method\": \"disconnect\",\n"
//										+ "  \"params\":\n" + "  {\n"
//										+ "    \"source\":\n" + "    {\n"
//										+ "      \"id\": 1234,\n"
//										+ "      \"token\": \"asdf\"\n"
//										+ "    },\n" + "    \"sink\":\n"
//										+ "    {\n" + "      \"id\": 5678,\n"
//										+ "      \"token\": \"qwer\"\n"
//										+ "    }\n" + "  }\n" + "}" },
//						{
//								"createMixerEndPoint1",
//								"{\n"
//										+ "  \"jsonrpc\": \"2.0\",\n"
//										+ "  \"id\": 2,\n"
//										+ "\n"
//										+ "  \"method\": \"createMixerEndPoint\",\n"
//										+ "  \"params\":\n" + "  {\n"
//										+ "    \"mixer\":\n" + "    {\n"
//										+ "      \"id\": 1234,\n"
//										+ "      \"token\": \"asdf\"\n"
//										+ "    }\n" + "  }\n" + "}" },
//						{
//								"createMixerEndPoint2",
//								"{\n"
//										+ "  \"jsonrpc\": \"2.0\",\n"
//										+ "  \"id\": 2,\n"
//										+ "\n"
//										+ "  \"method\": \"createMixerEndPoint\",\n"
//										+ "  \"params\":\n" + "  {\n"
//										+ "    \"mixer\":\n" + "    {\n"
//										+ "      \"id\": 1234,\n"
//										+ "      \"token\": \"asdf\"\n"
//										+ "    },\n" + "    \"params\": {}\n"
//										+ "  }\n" + "}" } });
//	}
//
//	private static MethodResolver methodResolver;
//
//	private String request;
//
//	@BeforeClass
//	public static void initLookupResolver() {
//		methodResolver = new MethodResolver("127.0.0.1", 333);
//	}
//
//	public RequestResolutionTest(String requestMethod, String request) {
//		this.request = request;
//	}
//
//	@Test
//	public void methodTest() {
//
//		try {
//
//			JsonObject requestJson = createJsonObject(request);
//
//			ThriftMethod method = lookupMethod(requestJson);
//
//			Assert.assertNotNull("Method not found", method);
//
//			JsonElement paramsJsonElem = requestJson.get("params");
//			if (paramsJsonElem != null) {
//
//				JsonObject params = paramsJsonElem.getAsJsonObject();
//				Object[] parsedParams = method.parseParamsFromRequest(params);
//
//				Assert.assertNotNull("Params can be parsed", parsedParams);
//			}
//
//		} catch (MethodNotFoundException e) {
//			Assert.fail(e.getMessage());
//		}
//	}
//
//	public ThriftMethod lookupMethod(JsonObject requestJson) {
//
//		JsonElement paramsProp = requestJson.get("params");
//		JsonObject params = null;
//		if (paramsProp != null) {
//			params = paramsProp.getAsJsonObject();
//		}
//
//		String method = requestJson.get("method").getAsString();
//
//		return methodResolver.lookup(method, params);
//	}
//
//	private JsonObject createJsonObject(String jsonStr) {
//		return JsonUtils.fromJson(jsonStr, JsonObject.class);
//	}
//
// }
