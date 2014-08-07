package org.kurento.client.internal.test;

import org.kurento.client.internal.transport.jsonrpc.RomServerJsonRpcHandler;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.thrift.jsonrpcconnector.JsonRpcClientThrift;
import org.kurento.thrift.jsonrpcconnector.JsonRpcServerThrift;

public class ThriftRomTest extends AbstractRomTest {

	private JsonRpcServerThrift server;

	@Override
	protected JsonRpcClient createJsonRpcClient() {
		return new JsonRpcClientThrift("127.0.0.1", 6463, "127.0.0.1", 7979);
	}

	@Override
	protected void startJsonRpcServer(RomServerJsonRpcHandler jsonRpcHandler) {
		server = new JsonRpcServerThrift(jsonRpcHandler, "127.0.0.1", 6463);
		server.start();
	}

	@Override
	protected void destroyJsonRpcServer() {
		server.destroy();
	}
}
