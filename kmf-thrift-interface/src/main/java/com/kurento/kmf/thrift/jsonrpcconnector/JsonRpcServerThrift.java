package com.kurento.kmf.thrift.jsonrpcconnector;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kurento.kmf.jsonrpcconnector.JsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.JsonUtils;
import com.kurento.kmf.jsonrpcconnector.internal.JsonRpcHandlerManager;
import com.kurento.kmf.jsonrpcconnector.internal.client.TransactionImpl;
import com.kurento.kmf.jsonrpcconnector.internal.client.TransactionImpl.ResponseSender;
import com.kurento.kmf.jsonrpcconnector.internal.message.*;
import com.kurento.kmf.jsonrpcconnector.internal.server.ServerSession;
import com.kurento.kmf.thrift.*;
import com.kurento.kmf.thrift.internal.ThriftInterfaceExecutorService;
import com.kurento.kms.thrift.api.KmsMediaServerService.Iface;
import com.kurento.kms.thrift.api.KmsMediaServerService.Processor;

public class JsonRpcServerThrift {

	private static Logger log = LoggerFactory
			.getLogger(JsonRpcServerThrift.class);

	private ThriftServer server;

	private JsonRpcHandler<?> handler;

	private ServerSession session;

	private Class<?> paramsClass;

	public JsonRpcServerThrift(JsonRpcHandler<?> jsonRpcHandler,
			String serverAddress, int serverPort) {

		this(jsonRpcHandler, new ThriftInterfaceExecutorService(
				new ThriftInterfaceConfiguration(serverAddress, serverPort)),
				new InetSocketAddress(serverAddress, serverPort));
	}

	public JsonRpcServerThrift(JsonRpcHandler<?> jsonRpcHandler,
			ThriftInterfaceExecutorService executorService,
			InetSocketAddress inetSocketAddress) {

		this.handler = jsonRpcHandler;
		this.paramsClass = JsonRpcHandlerManager.getParamsType(handler
				.getHandlerType());

		log.info("Starting JsonRpcServer on {}", inetSocketAddress);

		Processor<Iface> serverProcessor = new Processor<Iface>(new Iface() {

			@Override
			public String invokeJsonRpc(final String requestStr)
					throws TException {

				Request<?> request = JsonUtils.fromJsonRequest(requestStr,
						paramsClass);

				Response<JsonObject> response = processRequest(request);

				return response.toString();
			}
		});

		session = new ServerSession("XXX", null, null, "YYY") {
			@Override
			public void handleResponse(Response<JsonElement> response) {
				log.error("Trying to send a response from by means of session but it is not supported");
			}
		};

		server = new ThriftServer(serverProcessor, executorService,
				inetSocketAddress);
	}

	/**
	 * Process a request received through the thrift interface.
	 *
	 * @param request
	 * @return a response to the request
	 */
	@SuppressWarnings("unchecked")
	public Response<JsonObject> processRequest(Request<?> request) {

		log.trace("Req-> {}", request);

		final Response<JsonObject>[] response = new Response[1];

		TransactionImpl t = new TransactionImpl(session, request,
				new ResponseSender() {
			@Override
			public void sendResponse(Message message)
					throws IOException {
				response[0] = (Response<JsonObject>) message;
			}
		});

		try {

			@SuppressWarnings("rawtypes")
			JsonRpcHandler genericHandler = handler;
			genericHandler.handleRequest(t, request);

		} catch (Exception e) {

			ResponseError error = ResponseError.newFromException(e);
			return new Response<>(request.getId(), error);
		}

		if (response[0] != null) {
			// Simulate receiving json string from net
			String jsonResponse = response[0].toString();

			log.debug("<-Res {}", jsonResponse);

			Response<JsonObject> newResponse = JsonUtils.fromJsonResponse(
					jsonResponse, JsonObject.class);

			newResponse.setId(request.getId());

			return newResponse;

		}

		return new Response<>(request.getId());

	}

	/**
	 * Starts the thrift server
	 *
	 * @throws ThriftServerException
	 *             in case of error during creation
	 */
	public void start() {
		server.start();
		log.info("Thrift Server started");
	}

	public void destroy() {
		if (server != null) {
			server.destroy();
		}
	}

}
