package org.kurento.client.internal.test;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.kurento.client.internal.client.RemoteObjectFactory;
import org.kurento.client.internal.client.RemoteObjectTypedFactory;
import org.kurento.client.internal.test.model.Sample2;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomServerJsonRpcHandler;
import org.kurento.jsonrpc.client.JsonRpcClientLocal;

public class ConstructionTest {

	protected static RemoteObjectTypedFactory factory;

	@BeforeClass
	public static void initFactory() {
		factory = new RemoteObjectTypedFactory(new RemoteObjectFactory(
				new RomClientJsonRpcClient(new JsonRpcClientLocal(
						new RomServerJsonRpcHandler(
								"org.kurento.client.internal.test.model", "Impl")))));
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
