package com.kurento.tool.rom.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.internal.server.config.JsonRpcConfiguration;
import com.kurento.kmf.media.MediaApiConfiguration;
import com.kurento.kmf.thrift.ThriftInterfaceConfiguration;
import com.kurento.kmf.thrift.internal.ThriftInterfaceExecutorService;
import com.kurento.kmf.thrift.jsonrpcconnector.JsonRpcClientThrift;
import com.kurento.kmf.thrift.jsonrpcconnector.JsonRpcServerThrift;
import com.kurento.kmf.thrift.pool.MediaServerClientPoolService;
import com.kurento.tool.rom.client.RemoteObjectFactory;
import com.kurento.tool.rom.client.RemoteObjectTypedFactory;
import com.kurento.tool.rom.test.model.client.ComplexParam;
import com.kurento.tool.rom.test.model.client.SampleClass;
import com.kurento.tool.rom.test.model.client.SampleEnum;
import com.kurento.tool.rom.transport.jsonrpcconnector.RomClientJsonRpcClient;
import com.kurento.tool.rom.transport.jsonrpcconnector.RomServerJsonRpcHandler;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ThriftAbstractRomTest.KmfThriftTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class ThriftAbstractRomTest {

	@Configuration
	@ComponentScan(value = "com.kurento.kmf", basePackageClasses = { JsonRpcConfiguration.class })
	public static class KmfThriftTestConfiguration {

		@Bean
		public ThriftInterfaceConfiguration thriftInterfaceConfiguration() {

			ThriftInterfaceConfiguration configuration = new ThriftInterfaceConfiguration();
			configuration.setServerAddress("127.0.0.1");
			configuration.setServerPort(6464);
			return configuration;
		}

		@Bean
		public MediaApiConfiguration mediaApiConfiguration() {
			return new MediaApiConfiguration();
		}
	}

	private static Logger LOG = LoggerFactory
			.getLogger(ThriftAbstractRomTest.class);

	@Autowired
	private MediaServerClientPoolService clientPool;

	@Autowired
	private ThriftInterfaceExecutorService executorService;

	private SampleClass obj;

	private JsonRpcServerThrift server;

	private RemoteObjectTypedFactory factory;

	@PostConstruct
	public void initObject() {

		LOG.info("Starting server");
		server = new JsonRpcServerThrift(new RomServerJsonRpcHandler(
				"com.kurento.tool.rom.test.model.server", "Impl"),
				executorService, new InetSocketAddress("127.0.0.1", 6464));

		server.start();
		LOG.info("Server started");

		LOG.info("Starting client");

		JsonRpcClient client = new JsonRpcClientThrift(clientPool,
				executorService, new InetSocketAddress("127.0.0.1", 7979));

		factory = new RemoteObjectTypedFactory(new RemoteObjectFactory(
				new RomClientJsonRpcClient(client)));

		// RemoteObjectTypedFactory factory = new RemoteObjectTypedFactory(
		// new RemoteObjectFactory(new RomClientJsonRpcClient(
		// new JsonRpcClientLocal(new RomServerJsonRpcHandler(
		// "com.kurento.tool.rom.test.model.server", "Impl")))));

	}

	@PreDestroy
	public void destroy() {
		LOG.info("Destroying Thrift server");
		server.destroy();
		LOG.info("Thrift server destroyed");
	}

	@Test
	public void constructionTest() {

		obj = factory.getFactory(SampleClass.Factory.class)
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
