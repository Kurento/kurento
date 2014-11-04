package org.kurento.client.internal.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.test.model.client.ComplexParam;
import org.kurento.client.internal.test.model.client.SampleClass;
import org.kurento.client.internal.test.model.client.SampleEnum;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomServerJsonRpcHandler;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRomTest {

	private static Logger LOG = LoggerFactory.getLogger(AbstractRomTest.class);

	@Test
	public void test() throws IOException {

		LOG.info("Starting server");

		RomServerJsonRpcHandler jsonRpcHandler = new RomServerJsonRpcHandler(
				"org.kurento.client.internal.test.model.server", "Impl");

		startJsonRpcServer(jsonRpcHandler);

		LOG.info("Server started");

		LOG.info("Starting client");

		JsonRpcClient client = createJsonRpcClient();
		useRom(client);

		client.close();

		destroyJsonRpcServer();
	}

	protected abstract void destroyJsonRpcServer();

	protected abstract JsonRpcClient createJsonRpcClient();

	protected abstract void startJsonRpcServer(
			RomServerJsonRpcHandler jsonRpcHandler);

	public void useRom(JsonRpcClient client) {

		RomManager manager = new RomManager(new RomClientJsonRpcClient(client));

		SampleClass obj = new SampleClass.Builder("XXX", false, manager)
				.withAtt3(0.5f).withAtt4(22).build();

		for (int i = 0; i < 5; i++) {

			assertEquals(obj.getAtt1(), "XXX");
			assertFalse(obj.getAtt2());
			assertEquals(obj.getAtt3(), 0.5f, 0.01);
			assertEquals(obj.getAtt4(), 22);

			assertEquals(SampleEnum.CONSTANT_1,
					obj.echoEnum(SampleEnum.CONSTANT_1));

			ComplexParam returnValue = obj.echoRegister(new ComplexParam(
					"prop1", 33));

			assertEquals(returnValue.getProp1(), "prop1");
			assertEquals(returnValue.getProp2(), 33);

			List<SampleEnum> result = obj.echoListEnum(Arrays.asList(
					SampleEnum.CONSTANT_1, SampleEnum.CONSTANT_2));

			assertEquals(SampleEnum.CONSTANT_1, result.get(0));
			assertEquals(SampleEnum.CONSTANT_2, result.get(1));

			List<ComplexParam> params = new ArrayList<>();
			params.add(new ComplexParam("prop1_1", 33));
			params.add(new ComplexParam("prop1_2", 44));

			List<ComplexParam> returnParams = obj.echoListRegister(params);

			ComplexParam value1 = returnParams.get(0);
			ComplexParam value2 = returnParams.get(1);

			assertEquals(value1.getProp1(), "prop1_1");
			assertEquals(value1.getProp2(), 33);

			assertEquals(value2.getProp1(), "prop1_2");
			assertEquals(value2.getProp2(), 44);

		}
	}
}