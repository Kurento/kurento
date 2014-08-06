package org.kurento.tool.rom.test;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.kurento.kmf.jsonrpcconnector.client.JsonRpcClientLocal;
import org.kurento.tool.rom.client.RemoteObjectFactory;
import org.kurento.tool.rom.client.RemoteObjectTypedFactory;
import org.kurento.tool.rom.server.ProtocolException;
import org.kurento.tool.rom.test.model.SampleRemoteClass;
import org.kurento.tool.rom.transport.jsonrpcconnector.RomClientJsonRpcClient;
import org.kurento.tool.rom.transport.jsonrpcconnector.RomServerJsonRpcHandler;

public class RomInterfaceImplTest {

	protected static RemoteObjectTypedFactory factory;

	@BeforeClass
	public static void initFactory() {
		factory = new RemoteObjectTypedFactory(new RemoteObjectFactory(
				new RomClientJsonRpcClient(new JsonRpcClientLocal(
						new RomServerJsonRpcHandler(
								"org.kurento.tool.rom.test.model", "Impl")))));
	}

	private SampleRemoteClass obj;

	@Before
	public void initObject() {
		obj = factory.create(SampleRemoteClass.class);
	}

	@Test
	public void voidReturnMethodTest() throws ProtocolException {
		obj.methodReturnVoid();
	}

	@Test
	public void stringReturnMethodTest() throws ProtocolException {
		assertEquals(obj.methodReturnsString(), "XXXX");
	}

	@Test
	public void intReturnMethodTest() throws ProtocolException {
		assertEquals(obj.methodReturnsInt(), 0);
	}

	@Test
	public void booleanReturnMethodTest() throws ProtocolException {
		assertEquals(obj.methodReturnsBoolean(), false);
	}

	@Test
	public void floatReturnMethodTest() throws ProtocolException {
		assertEquals(obj.methodReturnsFloat(), 0.5f, 0.01);
	}

	@Test
	public void stringParamMethodTest() throws ProtocolException {
		assertEquals(obj.methodParamString("XXXX"), "XXXX");
	}

	@Test
	public void intParamMethodTest() throws ProtocolException {
		assertEquals(obj.methodParamInt(55), 55);
	}

	@Test
	public void booleanParamMethodTest() throws ProtocolException {
		assertEquals(obj.methodParamBoolean(true), true);
	}

	@Test
	public void floatParamMethodTest() throws ProtocolException {
		assertEquals(obj.methodParamFloat(0.5f), 0.5f, 0.01);
	}

}
