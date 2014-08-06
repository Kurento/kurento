package org.kurento.tool.rom.test;

import org.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import org.kurento.kmf.thrift.jsonrpcconnector.JsonRpcClientThrift;
import org.kurento.kmf.thrift.jsonrpcconnector.JsonRpcServerThrift;
import org.kurento.tool.rom.transport.jsonrpcconnector.RomServerJsonRpcHandler;

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
