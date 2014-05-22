package com.kurento.tool.rom.test;

import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.thrift.jsonrpcconnector.JsonRpcClientThrift;
import com.kurento.kmf.thrift.jsonrpcconnector.JsonRpcServerThrift;
import com.kurento.tool.rom.transport.jsonrpcconnector.RomServerJsonRpcHandler;

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
