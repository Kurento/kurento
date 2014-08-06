package org.kurento.tool.rom.test;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.kurento.jsonrpcconnector.client.JsonRpcClientLocal;
import org.kurento.tool.rom.client.RemoteObjectFactory;
import org.kurento.tool.rom.client.RemoteObjectTypedFactory;
import org.kurento.tool.rom.test.model.Sample2;
import org.kurento.tool.rom.transport.jsonrpcconnector.RomClientJsonRpcClient;
import org.kurento.tool.rom.transport.jsonrpcconnector.RomServerJsonRpcHandler;

public class ConstructionTest {

	protected static RemoteObjectTypedFactory factory;

	@BeforeClass
	public static void initFactory() {
		factory = new RemoteObjectTypedFactory(new RemoteObjectFactory(
				new RomClientJsonRpcClient(new JsonRpcClientLocal(
						new RomServerJsonRpcHandler(
								"org.kurento.tool.rom.test.model", "Impl")))));
	}

	@Test
	public void initObject() {

		Sample2 obj = factory.getFactory(Sample2.Factory.class)
				.create("XXX", 33).withAtt3(0.5f).att4().build();

		String att1 = obj.getAtt1();
		int att2 = obj.getAtt2();
		float att3 = obj.getAtt3();
		boolean att4 = obj.getAtt4();

		assertEquals(att1, "XXX");
		assertEquals(att2, 33);
		assertEquals(att3, 0.5f, 0.01);
		assertEquals(att4, true);

	}

}
