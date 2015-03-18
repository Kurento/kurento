package org.kurento.client.internal.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.test.model.client.ComplexParam;
import org.kurento.client.internal.test.model.client.SampleClass;
import org.kurento.client.internal.test.model.client.SampleEnum;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomServerJsonRpcHandler;
import org.kurento.jsonrpc.client.JsonRpcClientLocal;

import com.google.common.collect.Maps;

public class SyncConstMethodsTest {

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

	private SampleClass obj;

	@Before
	public void initObject() {
		obj = new SampleClass.Builder("XXX", false, manager).withAtt3(0.5f)
				.withAtt4(22).build();
	}

	@Test
	public void constructionTest() {
		assertEquals(obj.getAtt1(), "XXX");
		assertFalse(obj.getAtt2());
		assertEquals(obj.getAtt3(), 0.5f, 0.01);
		assertEquals(obj.getAtt4(), 22);
	}

	@Test
	public void echoEnumTest() {
		assertEquals(SampleEnum.CONSTANT_1, obj.echoEnum(SampleEnum.CONSTANT_1));
	}

	@Test
	public void echoRegisterTest() {

		ComplexParam returnValue = obj.echoRegister(new ComplexParam("prop1",
				33));

		assertEquals(returnValue.getProp1(), "prop1");
		assertEquals(returnValue.getProp2(), 33);
	}

	@Test
	public void echoEnumListTest() {

		List<SampleEnum> result = obj.echoListEnum(Arrays.asList(
				SampleEnum.CONSTANT_1, SampleEnum.CONSTANT_2));

		assertEquals(SampleEnum.CONSTANT_1, result.get(0));
		assertEquals(SampleEnum.CONSTANT_2, result.get(1));
	}

	@Test
	public void echoRegisterListTest() {

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

	@Test
	public void echoEnumMapTest() {

		Map<String, SampleEnum> init = new HashMap<String, SampleEnum>();
		init.put("value1", SampleEnum.CONSTANT_1);
		init.put("value2", SampleEnum.CONSTANT_2);

		Map<String, SampleEnum> result = obj.echoMapEnum(Maps.newHashMap(init));

		assertEquals(SampleEnum.CONSTANT_1, result.get("value1"));
		assertEquals(SampleEnum.CONSTANT_2, result.get("value2"));
	}

	@Test
	public void echoRegisterMapTest() {

		Map<String, ComplexParam> params = new HashMap<String, ComplexParam>();
		params.put("value1", new ComplexParam("prop1_1", 33));
		params.put("value2", new ComplexParam("prop1_2", 44));

		Map<String, ComplexParam> returnParams = obj.echoMapRegister(params);

		ComplexParam value1 = returnParams.get("value1");
		ComplexParam value2 = returnParams.get("value2");

		assertEquals(value1.getProp1(), "prop1_1");
		assertEquals(value1.getProp2(), 33);

		assertEquals(value2.getProp1(), "prop1_2");
		assertEquals(value2.getProp2(), 44);
	}

}
