package org.kurento.client.internal.test;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.test.model.client.SampleClass;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomServerJsonRpcHandler;
import org.kurento.jsonrpc.client.JsonRpcClientLocal;

public class ReturnReferencesTest {

	private static RomManager manager;

	@BeforeClass
	public static void initFactory() {
		manager = new RomManager(
				new RomClientJsonRpcClient(
						new JsonRpcClientLocal(
								new RomServerJsonRpcHandler(
										"org.kurento.client.internal.test.model.server",
										"Impl"))));
	}

	@Test
	public void objectRefTest() {

		SampleClass obj = new SampleClass.Builder("AAA", false, manager)
				.withAtt3(0.5f).withAtt4(22).build();

		SampleClass obj2 = new SampleClass.Builder("BBB", false, manager)
				.withAtt3(0.5f).withAtt4(22).build();

		SampleClass obj3 = obj.echoObjectRef(obj2);

		assertEquals(obj3.getAtt1(), obj2.getAtt1());
		assertEquals(obj3.getAtt2(), obj2.getAtt2());
	}

	@Test
	public void objectRefTestAsync() throws InterruptedException {

		SampleClass obj = new SampleClass.Builder("AAA", false, manager)
				.withAtt3(0.5f).withAtt4(22).build();

		final SampleClass obj2 = new SampleClass.Builder("BBB", false, manager)
				.withAtt3(0.5f).withAtt4(22).build();

		SampleClass obj3 = obj.echoObjectRef(obj2);

		Assert.assertNotNull(obj3);

		assertEquals(obj3.getAtt1(), obj2.getAtt1());
		assertEquals(obj3.getAtt2(), obj2.getAtt2());
	}

}
