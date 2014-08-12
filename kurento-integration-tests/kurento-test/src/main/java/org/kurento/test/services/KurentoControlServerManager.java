package org.kurento.test.services;

import org.kurento.control.server.KurentoControlServerApp;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.springframework.context.ConfigurableApplicationContext;

public class KurentoControlServerManager {

	private ConfigurableApplicationContext context;

	public KurentoControlServerManager(JsonRpcClient client, int httpPort) {

		KurentoControlServerApp.setJsonRpcClient(client);

		System.setProperty(KurentoControlServerApp.WEBSOCKET_PORT_PROPERTY,
				Integer.toString(httpPort));

		context = KurentoControlServerApp.start();
	}

	public void destroy() {
		context.close();
	}
}
