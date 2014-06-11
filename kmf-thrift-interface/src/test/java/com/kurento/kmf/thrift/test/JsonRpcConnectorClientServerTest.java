package com.kurento.kmf.thrift.test;

import java.io.IOException;

import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.thrift.jsonrpcconnector.JsonRpcClientThrift;
import com.kurento.kmf.thrift.jsonrpcconnector.JsonRpcServerThrift;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = JsonRpcConnectorClientServerTest.KmfThriftTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class JsonRpcConnectorClientServerTest {

	// @Configuration
	// @ComponentScan(value = "com.kurento.kmf", basePackageClasses = {
	// com.kurento.kmf.jsonrpcconnector.internal.server.config.JsonRpcConfiguration.class
	// })
	// public static class KmfThriftTestConfiguration {
	//
	// @Bean
	// public ThriftInterfaceConfiguration thriftInterfaceConfiguration() {
	//
	// ThriftInterfaceConfiguration configuration = new
	// ThriftInterfaceConfiguration();
	// configuration.setServerAddress("127.0.0.1");
	// configuration.setServerPort(9292);
	// return configuration;
	// }
	//
	// }

	static class Params {
		String param1;
		String param2;
	}

	private static Logger LOG = LoggerFactory
			.getLogger(JsonRpcConnectorClientServerTest.class);

	private static class EchoJsonRpcHandler extends
			DefaultJsonRpcHandler<Params> {

		@Override
		public void handleRequest(Transaction transaction,
				Request<Params> request) throws Exception {

			transaction.sendResponse(request.getParams());
		}
	}

	@Test
	public void test() throws TException, IOException {

		LOG.info("Starting server");
		JsonRpcServerThrift server = new JsonRpcServerThrift(
				new EchoJsonRpcHandler(), "127.0.0.1", 19292);

		server.start();
		LOG.info("Server started");

		LOG.info("Starting client");

		JsonRpcClient client = new JsonRpcClientThrift("127.0.0.1", 19292,
				"127.0.0.1", 7979);

		Params params = new Params();
		params.param1 = "Value1";
		params.param2 = "Value2";

		Params result = client.sendRequest("echo", params, Params.class);

		LOG.info("Response:" + result);

		Assert.assertEquals(params.param1, result.param1);
		Assert.assertEquals(params.param2, result.param2);

		client.close();

		LOG.info("Client finished");

		server.destroy();

		LOG.info("Server finished");

	}
}
