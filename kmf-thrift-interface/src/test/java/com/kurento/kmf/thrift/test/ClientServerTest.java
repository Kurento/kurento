package com.kurento.kmf.thrift.test;

import java.net.InetSocketAddress;

import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Test;

import com.kurento.kmf.thrift.ThriftInterfaceConfiguration;
import com.kurento.kmf.thrift.ThriftServer;
import com.kurento.kmf.thrift.internal.ThriftInterfaceExecutorService;
import com.kurento.kmf.thrift.pool.ThriftClientPoolService;
import com.kurento.kms.thrift.api.KmsMediaHandlerService;
import com.kurento.kms.thrift.api.KmsMediaServerService;
import com.kurento.kms.thrift.api.KmsMediaServerService.Client;

public class ClientServerTest {

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

		ThriftInterfaceConfiguration cfg = new ThriftInterfaceConfiguration();
		cfg.setServerAddress("127.0.0.1");
		cfg.setServerPort(9191);

		ThriftClientPoolService clientPool = new ThriftClientPoolService(
				cfg);

		ThriftInterfaceExecutorService executorService = new ThriftInterfaceExecutorService(
				cfg);

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
