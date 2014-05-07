package kmf.broker.server;

import java.io.IOException;
import java.net.InetSocketAddress;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.media.MediaApiConfiguration;
import com.kurento.kmf.thrift.ThriftInterfaceConfiguration;
import com.kurento.kmf.thrift.internal.ThriftInterfaceExecutorService;
import com.kurento.kmf.thrift.jsonrpcconnector.JsonRpcClientThrift;
import com.kurento.kmf.thrift.pool.MediaServerAsyncClientFactory;
import com.kurento.kmf.thrift.pool.MediaServerAsyncClientPool;
import com.kurento.kmf.thrift.pool.MediaServerClientPoolService;
import com.kurento.kmf.thrift.pool.MediaServerSyncClientFactory;
import com.kurento.kmf.thrift.pool.MediaServerSyncClientPool;

public class MediaServerBroker {

	private static final Logger LOG = LoggerFactory
			.getLogger(MediaServerBroker.class);

	@Autowired
	private MediaApiConfiguration config;

	@Autowired
	private MediaServerClientPoolService clientPool;

	@Autowired
	private ThriftInterfaceExecutorService executorService;

	private JsonRpcServerBroker serverBroker;

	private JsonRpcClient client;

	public MediaServerBroker() {

	}

	// Used in non Spring environments
	public MediaServerBroker(String serverAddress, int serverPort,
			String handlerAddress, int handlerPort) {

		LOG.info(
				"Creating pipeline factory in non-spring environment with server {}:{} and handler {}:{}",
				serverAddress, serverPort, handlerAddress, handlerPort);

		this.config = new MediaApiConfiguration();

		this.config.setHandlerAddress(handlerAddress);
		this.config.setHandlerPort(handlerPort);

		ThriftInterfaceConfiguration cfg = new ThriftInterfaceConfiguration(
				serverAddress, serverPort);

		MediaServerAsyncClientPool asyncClientPool = new MediaServerAsyncClientPool(
				new MediaServerAsyncClientFactory(cfg), cfg);

		MediaServerSyncClientPool syncClientPool = new MediaServerSyncClientPool(
				new MediaServerSyncClientFactory(cfg), cfg);

		this.clientPool = new MediaServerClientPoolService(asyncClientPool,
				syncClientPool);

		this.executorService = new ThriftInterfaceExecutorService(cfg);

		init();
	}

	@PostConstruct
	private void init() {

		LOG.info("Starting Media Connector");
		
		if (client == null) {

			this.client = new JsonRpcClientThrift(
					clientPool,
					executorService,
					new InetSocketAddress(
							config.getHandlerAddress(), config.getHandlerPort()));

		}

		this.serverBroker = new JsonRpcServerBroker(client);
		this.serverBroker.start();
		
		LOG.info("Media Connector started");
	}
	
	@PreDestroy
	public void destroy() throws IOException{
		this.client.close();
		this.serverBroker.destroy();
	}
}
