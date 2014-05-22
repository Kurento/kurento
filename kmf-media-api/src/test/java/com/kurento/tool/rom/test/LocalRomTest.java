package com.kurento.tool.rom.test;

import com.kurento.kmf.jsonrpcconnector.JsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClientLocal;
import com.kurento.tool.rom.transport.jsonrpcconnector.RomServerJsonRpcHandler;

public class LocalRomTest extends AbstractRomTest {

	private JsonRpcHandler<? extends Object> handler;

	@Override
	protected JsonRpcClient createJsonRpcClient() {
		return new JsonRpcClientLocal(handler);
	}

	@Override
	protected void startJsonRpcServer(RomServerJsonRpcHandler jsonRpcHandler) {
		this.handler = jsonRpcHandler;
	}

	@Override
	protected void destroyJsonRpcServer() {

	}

}
