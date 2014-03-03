package com.kurento.kmf.thrift.test;

import java.net.InetSocketAddress;

import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.kurento.kmf.thrift.ThriftInterfaceConfiguration;
import com.kurento.kmf.thrift.ThriftServer;
import com.kurento.kmf.thrift.internal.ThriftInterfaceExecutorService;
import com.kurento.kmf.thrift.pool.MediaServerClientPoolService;
import com.kurento.kms.thrift.api.KmsMediaHandlerService;
import com.kurento.kms.thrift.api.KmsMediaServerService;
import com.kurento.kms.thrift.api.KmsMediaServerService.Client;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ClientServerTest.KmfThriftTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class ClientServerTest {

	@Configuration
	@ComponentScan("com.kurento.kmf")
	public static class KmfThriftTestConfiguration {

		@Bean
		public ThriftInterfaceConfiguration thriftInterfaceConfiguration() {

			ThriftInterfaceConfiguration configuration = new ThriftInterfaceConfiguration();
			configuration.setServerAddress("127.0.0.1");
			configuration.setServerPort(9191);
			return configuration;
		}
	}

	@Autowired
	private MediaServerClientPoolService clientPool;

	@Autowired
	private ThriftInterfaceExecutorService executorService;

	private final KmsMediaHandlerService.Processor<KmsMediaHandlerService.Iface> clientProcessor = new KmsMediaHandlerService.Processor<KmsMediaHandlerService.Iface>(
			new KmsMediaHandlerService.Iface() {
				@Override
				public void eventJsonRpc(String request) throws TException {
					System.out.println("Request received: " + request);
				}
			});

	KmsMediaServerService.Processor<KmsMediaServerService.Iface> serverProcessor = new KmsMediaServerService.Processor<KmsMediaServerService.Iface>(
			new KmsMediaServerService.Iface() {
				@Override
				public String invokeJsonRpc(String request) throws TException {
					return request;
				}
			});

	@Test
	public void test() throws TException {

		ThriftServer clientServer = new ThriftServer(clientProcessor,
				executorService, new InetSocketAddress("127.0.0.1", 7979));
		clientServer.start();

		ThriftServer server = new ThriftServer(serverProcessor,
				executorService, new InetSocketAddress("127.0.0.1", 9191));
		server.start();

		Client client = clientPool.acquireSync();

		String message = "Test echo message";

		String result = client.invokeJsonRpc(message);

		Assert.assertEquals(message, result);

		clientServer.destroy();
		server.destroy();
	}

}
