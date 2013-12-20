package com.kurento.kmf.jsonrpcconnector.server;

import com.kurento.kmf.jsonrpcconnector.JsonRpcHandler;

public interface JsonRpcHandlerRegistry {

	/**
	 * Configure a JsonRpcHandler at the specified URL paths.
	 */
	JsonRpcHandlerRegistration addHandler(JsonRpcHandler<?> jsonRpcHandler,
			String... paths);

	JsonRpcHandlerRegistration addPerSessionHandler(
			Class<? extends JsonRpcHandler<?>> handlerClass, String... string);

	JsonRpcHandlerRegistration addPerSessionHandler(String beanName,
			String... string);

}
