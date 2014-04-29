package com.kurento.tool.rom.test;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.kurento.kmf.jsonrpcconnector.Props;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClientLocal;
import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.client.RemoteObject;
import com.kurento.tool.rom.client.RemoteObjectFactory;
import com.kurento.tool.rom.server.Param;
import com.kurento.tool.rom.transport.jsonrpcconnector.RomClientJsonRpcClient;
import com.kurento.tool.rom.transport.jsonrpcconnector.RomServerJsonRpcHandler;

public class RomTest {

	@RemoteClass
	static public class RemoteClassTest {
		public String testMethod(@Param("param") String param) {
			return param;
		}
	}

	private static RemoteObjectFactory factory;

	@BeforeClass
	public static void initFactory() {
		factory = new RemoteObjectFactory(new RomClientJsonRpcClient(
				new JsonRpcClientLocal(new RomServerJsonRpcHandler(
						"com.kurento.tool.rom.test", ""))));
	}

	@Test
	public void simpleCreationTest() {

		RemoteObject obj = factory.create("RomTest$RemoteClassTest");

		String paramValue = "XXX";
		String result = obj.invoke("testMethod",
				new Props("param", paramValue), String.class);

		Assert.assertEquals(paramValue, result);
	}

	@Test
	public void creationReleaseTest() {

		RemoteObject obj = factory.create("RomTest$RemoteClassTest");

		obj.release();

		try {
			obj.invoke("testMethod", new Props("operationParams", new Props(
					"param", "XXX")), Void.class);

			Assert.fail("The invocation of a method in a released object should throw an exception");

		} catch (Exception e) {
		}
	}

	@RemoteClass
	static public class RemoteClassConstTest {

		private final String att;

		public RemoteClassConstTest(@Param("param") String param) {
			this.att = param;
		}

		public String getAtt() {
			return att;
		}
	}

	@Test
	public void consCreationTest() {

		String paramValue = "XXX";

		RemoteObject obj = factory.create("RomTest$RemoteClassConstTest",
				new Props("param", paramValue));

		String result = obj.invoke("getAtt", new Props("param", paramValue),
				String.class);

		Assert.assertEquals(paramValue, result);

	}

}
