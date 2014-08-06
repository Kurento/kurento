package org.kurento.kmf.thrift.jsonrpcconnector;

import static org.kurento.kmf.jsonrpcconnector.JsonUtils.fromJsonRequest;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.kurento.kmf.jsonrpcconnector.JsonUtils;
import org.kurento.kmf.jsonrpcconnector.KeepAliveManager;
import org.kurento.kmf.jsonrpcconnector.TransportException;
import org.kurento.kmf.jsonrpcconnector.client.Continuation;
import org.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import org.kurento.kmf.jsonrpcconnector.internal.JsonRpcRequestSenderHelper;
import org.kurento.kmf.jsonrpcconnector.internal.client.TransactionImpl.ResponseSender;
import org.kurento.kmf.jsonrpcconnector.internal.message.Message;
import org.kurento.kmf.jsonrpcconnector.internal.message.Request;
import org.kurento.kmf.jsonrpcconnector.internal.message.Response;
import org.kurento.kmf.thrift.ThriftInterfaceConfiguration;
import org.kurento.kmf.thrift.ThriftServer;
import org.kurento.kmf.thrift.ThriftTransportException;
import org.kurento.kmf.thrift.internal.ThriftInterfaceExecutorService;
import org.kurento.kmf.thrift.pool.ThriftClientPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kurento.kms.thrift.api.KmsMediaHandlerService.Iface;
import com.kurento.kms.thrift.api.KmsMediaHandlerService.Processor;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.invokeJsonRpc_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.Client;

public class JsonRpcClientThrift extends JsonRpcClient {

	private static final Logger log = LoggerFactory
			.getLogger(JsonRpcClientThrift.class);

	private ThriftClientPoolService clientPool;
	private ThriftServer server;
	private InetSocketAddress localHandlerAddress;

	private final ResponseSender dummyResponseSenderForEvents = new ResponseSender() {
		@Override
		public void sendResponse(Message message) throws IOException {
			log.warn(
					"The thrift client is trying to send the response '{}' for "
							+ "a request from server. But with Thrift it is not possible",
					message);
		}
	};

	public JsonRpcClientThrift() {
		this("127.0.0.1", 9090, "127.0.0.1", 9191);
	}

	public JsonRpcClientThrift(String serverAddress, int serverPort,
			String localAddress, int localPort) {

		this(new ThriftClientPoolService(new ThriftInterfaceConfiguration(
				serverAddress, serverPort)),
				new ThriftInterfaceExecutorService(
						new ThriftInterfaceConfiguration(serverAddress,
								serverPort)), new InetSocketAddress(
						localAddress, localPort));
	}

	public JsonRpcClientThrift(ThriftClientPoolService clientPool,
			ThriftInterfaceExecutorService executorService,
			InetSocketAddress localHandlerAddress) {

		this.clientPool = clientPool;

		this.localHandlerAddress = localHandlerAddress;

		this.rsHelper = new JsonRpcRequestSenderHelper() {
			@Override
			public <P, R> Response<R> internalSendRequest(Request<P> request,
					Class<R> resultClass) throws IOException {
				try {
					return internalSendRequestThrift(request, resultClass);
				} catch (ThriftTransportException e) {
					throw new TransportException(
							"Error sendind request to server", e);
				}
			}

			@Override
			protected void internalSendRequest(
					Request<? extends Object> request,
					Class<JsonElement> resultClass,
					Continuation<Response<JsonElement>> continuation) {
				internalSendRequestThrift(request, resultClass, continuation);
			}
		};

		final Processor<Iface> clientProcessor = new Processor<Iface>(
				new Iface() {
					@Override
					public void eventJsonRpc(String request) throws TException {

						try {

							log.debug("<-Req {}", request.trim());

							JsonObject message = JsonUtils.fromJson(request,
									JsonObject.class);

							handleRequestFromServer(message);

						} catch (Exception e) {
							log.error(
									"Exception while processing a request received from server",
									e);
						}
					}
				});

		server = new ThriftServer(clientProcessor, executorService,
				localHandlerAddress);

		server.start();

		keepAliveManager = new KeepAliveManager(this,
				KeepAliveManager.Mode.PER_CLIENT);

		keepAliveManager.start();
	}

	private void handleRequestFromServer(JsonObject message) throws IOException {
		handlerManager.handleRequest(session,
				fromJsonRequest(message, JsonElement.class),
				dummyResponseSenderForEvents);
	}

	private <P> void processRequest(Request<P> request) {
		// TODO Remove this hack -----------------------
		if (request.getMethod().equals("subscribe")) {

			log.debug(
					"Adding local address info to subscription request. ip:{} port:{}",
					localHandlerAddress.getHostString(),
					localHandlerAddress.getPort());

			JsonObject params = (JsonObject) request.getParams();
			params.addProperty("ip", localHandlerAddress.getHostString());
			params.addProperty("port",
					Integer.valueOf(localHandlerAddress.getPort()));
		}
		// ---------------------------------------------
	}

	/**
	 *
	 * @param request
	 *            the request
	 * @param resultClass
	 *            the expected result class
	 * @return The response from the media server
	 * @throws ThriftTransportException
	 *             if the request could not be sent to the media server due to a
	 *             problem in the transport
	 */
	public <P, R> Response<R> internalSendRequestThrift(Request<P> request,
			Class<R> resultClass) throws ThriftTransportException {

		Client client = clientPool.acquireSync();

		try {

			log.debug("Req-> {}", request);

			processRequest(request);

			String responseStr = client.invokeJsonRpc(request.toString());

			log.debug("<-Res {}", responseStr.trim());

			Response<R> response = JsonUtils.fromJsonResponse(responseStr,
					resultClass);

			return response;

		} catch (TException e) {
			throw new ThriftTransportException(
					"Error sending request to the remote server", e);
		} finally {
			clientPool.release(client);
		}
	}

	protected void internalSendRequestThrift(Request<? extends Object> request,
			final Class<JsonElement> resultClass,
			final Continuation<Response<JsonElement>> continuation) {

		log.debug("Req-> {}", request);

		processRequest(request);

		sendRequest(request, resultClass, continuation, true);
	}

	private void sendRequest(final Request<? extends Object> request,
			final Class<JsonElement> resultClass,
			final Continuation<Response<JsonElement>> continuation,
			final boolean retry) {

		final AsyncClient client = clientPool.acquireAsync();

		try {
			client.invokeJsonRpc(request.toString(),
					new AsyncMethodCallback<AsyncClient.invokeJsonRpc_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);

							log.error(
									"[Client] Error sending request to server",
									exception);

							if (retry && exception instanceof ConnectException) {
								sendRequest(request, resultClass, continuation,
										false);
							} else {
								continuation.onError(exception);
							}
						}

						@Override
						public void onComplete(invokeJsonRpc_call thriftResponse) {
							clientPool.release(client);

							try {
								String responseStr = thriftResponse.getResult();

								log.debug("<-Res {}", responseStr.trim());

								Response<JsonElement> response = JsonUtils
										.fromJsonResponse(responseStr,
												resultClass);

								continuation.onSuccess(response);

							} catch (TException e) {
								continuation.onError(e);
							}
						}

					});
		} catch (TException e) {
			log.error("Error on sendRequest", e);
			continuation.onError(e);
		}
	}

	@Override
	public void close() throws IOException {
		if (server != null) {
			server.destroy();
		}

		if (keepAliveManager != null) {
			keepAliveManager.stop();
		}
	}
}
