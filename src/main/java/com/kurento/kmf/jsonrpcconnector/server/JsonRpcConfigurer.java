package com.kurento.kmf.jsonrpcconnector.server;

import com.kurento.kmf.jsonrpcconnector.JsonRpcHandler;

public interface JsonRpcConfigurer {

	/**
	 * Register {@link JsonRpcHandler}s.
	 */
	void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry);

}
