package org.kurento.tool.rom.test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.kurento.kmf.jsonrpcconnector.client.JsonRpcClientLocal;
import org.kurento.kmf.media.Continuation;
import org.kurento.tool.rom.client.RemoteObjectFactory;
import org.kurento.tool.rom.client.RemoteObjectTypedFactory;
import org.kurento.tool.rom.test.model.client.SampleClass;
import org.kurento.tool.rom.transport.jsonrpcconnector.RomClientJsonRpcClient;
import org.kurento.tool.rom.transport.jsonrpcconnector.RomServerJsonRpcHandler;

public class ReturnReferencesTest {

	private static RemoteObjectTypedFactory factory;

	@BeforeClass
	public static void initFactory() {
		factory = new RemoteObjectTypedFactory(new RemoteObjectFactory(
				new RomClientJsonRpcClient(new JsonRpcClientLocal(
						new RomServerJsonRpcHandler(
								"org.kurento.tool.rom.test.model.server",
								"Impl")))));
	}

	@Test
	public void objectRefTest() {

		SampleClass obj = factory.getFactory(SampleClass.Factory.class)
				.create("AAA", false).withAtt3(0.5f).withAtt4(22).build();

		SampleClass obj2 = factory.getFactory(SampleClass.Factory.class)
				.create("BBB", false).withAtt3(0.5f).withAtt4(22).build();

		SampleClass obj3 = obj.echoObjectRef(obj2);

		assertEquals(obj3.getAtt1(), obj2.getAtt1());
		assertEquals(obj3.getAtt2(), obj2.getAtt2());
	}

	@Test
	public void objectRefTestAsync() throws InterruptedException {

		SampleClass obj = factory.getFactory(SampleClass.Factory.class)
				.create("AAA", false).withAtt3(0.5f).withAtt4(22).build();

		final SampleClass obj2 = factory.getFactory(SampleClass.Factory.class)
				.create("BBB", false).withAtt3(0.5f).withAtt4(22).build();

		final BlockingQueue<SampleClass> queue = new ArrayBlockingQueue<>(1);

		obj.echoObjectRef(obj2, new Continuation<SampleClass>() {

			@Override
			public void onSuccess(SampleClass obj3) {
				queue.add(obj3);
			}

			@Override
			public void onError(Throwable cause) {

			}
		});

		SampleClass obj3 = queue.poll(500, MILLISECONDS);

		Assert.assertNotNull(obj3);

		assertEquals(obj3.getAtt1(), obj2.getAtt1());
		assertEquals(obj3.getAtt2(), obj2.getAtt2());
	}

}
