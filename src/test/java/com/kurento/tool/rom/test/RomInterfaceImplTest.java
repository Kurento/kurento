package com.kurento.tool.rom.test;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClientLocal;
import com.kurento.tool.rom.client.RemoteObjectFactory;
import com.kurento.tool.rom.client.RemoteObjectTypedFactory;
import com.kurento.tool.rom.server.MediaApiException;
import com.kurento.tool.rom.test.model.SampleRemoteClass;
import com.kurento.tool.rom.transport.jsonrpcconnector.RomClientJsonRpcClient;
import com.kurento.tool.rom.transport.jsonrpcconnector.RomServerJsonRpcHandler;

public class RomInterfaceImplTest {

	protected static RemoteObjectTypedFactory factory;

	@BeforeClass
	public static void initFactory() {
		factory = new RemoteObjectTypedFactory(new RemoteObjectFactory(
				new RomClientJsonRpcClient(new JsonRpcClientLocal(
						new RomServerJsonRpcHandler(
								"com.kurento.tool.rom.test.model", "Impl")))));
	}

	private SampleRemoteClass obj;

	@Before
	public void initObject() {
		obj = factory.create(SampleRemoteClass.class);
	}

	@Test
	public void voidReturnMethodTest() throws MediaApiException {
		obj.methodReturnVoid();
	}

	@Test
	public void stringReturnMethodTest() throws MediaApiException {
		assertEquals(obj.methodReturnsString(), "XXXX");
	}

	@Test
	public void intReturnMethodTest() throws MediaApiException {
		assertEquals(obj.methodReturnsInt(), 0);
	}

	@Test
	public void booleanReturnMethodTest() throws MediaApiException {
		assertEquals(obj.methodReturnsBoolean(), false);
	}

	@Test
	public void floatReturnMethodTest() throws MediaApiException {
		assertEquals(obj.methodReturnsFloat(), 0.5f, 0.01);
	}

	@Test
	public void stringParamMethodTest() throws MediaApiException {
		assertEquals(obj.methodParamString("XXXX"), "XXXX");
	}

	@Test
	public void intParamMethodTest() throws MediaApiException {
		assertEquals(obj.methodParamInt(55), 55);
	}

	@Test
	public void booleanParamMethodTest() throws MediaApiException {
		assertEquals(obj.methodParamBoolean(true), true);
	}

	@Test
	public void floatParamMethodTest() throws MediaApiException {
		assertEquals(obj.methodParamFloat(0.5f), 0.5f, 0.01);
	}

}
