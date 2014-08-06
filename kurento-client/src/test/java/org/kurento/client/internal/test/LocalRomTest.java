package org.kurento.client.internal.test;

import org.kurento.client.internal.transport.jsonrpcconnector.RomServerJsonRpcHandler;
import org.kurento.jsonrpc.JsonRpcHandler;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientLocal;

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
