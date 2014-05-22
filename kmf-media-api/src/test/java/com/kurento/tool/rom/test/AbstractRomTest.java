package com.kurento.tool.rom.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.tool.rom.client.RemoteObjectFactory;
import com.kurento.tool.rom.client.RemoteObjectTypedFactory;
import com.kurento.tool.rom.test.model.client.ComplexParam;
import com.kurento.tool.rom.test.model.client.SampleClass;
import com.kurento.tool.rom.test.model.client.SampleEnum;
import com.kurento.tool.rom.transport.jsonrpcconnector.RomClientJsonRpcClient;
import com.kurento.tool.rom.transport.jsonrpcconnector.RomServerJsonRpcHandler;

public abstract class AbstractRomTest {

	private static Logger LOG = LoggerFactory.getLogger(ThriftRomTest.class);

	@Test
	public void test() throws IOException {

		LOG.info("Starting server");

		RomServerJsonRpcHandler jsonRpcHandler = new RomServerJsonRpcHandler(
				"com.kurento.tool.rom.test.model.server", "Impl");

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

		RemoteObjectTypedFactory factory = new RemoteObjectTypedFactory(
				new RemoteObjectFactory(new RomClientJsonRpcClient(client)));

		SampleClass obj = factory.getFactory(SampleClass.Factory.class)
				.create("XXX", false).withAtt3(0.5f).withAtt4(22).build();

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