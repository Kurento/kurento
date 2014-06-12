package com.kurento.kmf.thrift.jsonrpcconnector;

import static com.kurento.kmf.jsonrpcconnector.JsonUtils.fromJsonRequest;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kurento.kmf.jsonrpcconnector.JsonUtils;
import com.kurento.kmf.jsonrpcconnector.TransportException;
import com.kurento.kmf.jsonrpcconnector.client.Continuation;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.internal.JsonRpcRequestSenderHelper;
import com.kurento.kmf.jsonrpcconnector.internal.client.TransactionImpl.ResponseSender;
import com.kurento.kmf.jsonrpcconnector.internal.message.Message;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;
import com.kurento.kmf.thrift.ThriftInterfaceConfiguration;
import com.kurento.kmf.thrift.ThriftServer;
import com.kurento.kmf.thrift.ThriftTransportException;
import com.kurento.kmf.thrift.internal.ThriftInterfaceExecutorService;
import com.kurento.kmf.thrift.pool.ThriftClientPoolService;
import com.kurento.kms.thrift.api.KmsMediaHandlerService.Iface;
import com.kurento.kms.thrift.api.KmsMediaHandlerService.Processor;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.invokeJsonRpc_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.Client;

public class JsonRpcClientThrift extends JsonRpcClient {

	private static final Logger log = LoggerFactory
			.getLogger(JsonRpcClientThrift.class);

	public static final int KEEP_ALIVE_TIME = 120000;

	private ThriftClientPoolService clientPool;

	private final ResponseSender dummyResponseSenderForEvents = new ResponseSender() {
		@Override
		public void sendResponse(Message message) throws IOException {
			log.warn(
					"The thrift client is trying to send the response '{}' for "
							+ "a request from server. But with Thrift it is not possible",
					message);
		}
	};

	private ThriftServer server;
	private boolean stopKeepAlive;
	private final Set<String> sessions = new HashSet<>();

	private InetSocketAddress localHandlerAddress;

	private final Thread keepAliveThread = new Thread(new Runnable() {

		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(KEEP_ALIVE_TIME);
				} catch (InterruptedException e) {
					return;
				}

				synchronized (keepAliveThread) {
					if (stopKeepAlive) {
						return;
					}
				}

				Set<String> copiedSessions;

				synchronized (sessions) {
					copiedSessions = new HashSet<>(sessions);
				}

				/* sendKeepAlives */
				for (String copiedSession : copiedSessions) {
					int id = new Random().nextInt();
					Request<Void> request = new Request<>(copiedSession,
							Integer.valueOf(id), "keepAlive", null);

					log.info("Sending keep alive for session: {}",
							copiedSession);

					try {
						Response<Void> response = internalSendRequestThrift(
								request, Void.class);

						if (response.isError()) {
							log.error(
									"Error on session {} keep alive, removing",
									copiedSession);
							synchronized (sessions) {
								sessions.remove(copiedSession);
							}
						}
					} catch (TransportException e) {
						log.warn("Could not send keepalive for session {}",
								copiedSession, e);
					}
				}
			}
		}
	});

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
			protected void internalSendRequest(Request<Object> request,
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
		keepAliveThread.start();
	}

	private void handleRequestFromServer(JsonObject message) throws IOException {
		handlerManager.handleRequest(session,
				fromJsonRequest(message, JsonElement.class),
				dummyResponseSenderForEvents);
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

			String responseStr;

			responseStr = client.invokeJsonRpc(request.toString());

			log.debug("<-Res {}", responseStr.trim());

			Response<R> response = JsonUtils.fromJsonResponse(responseStr,
					resultClass);
			String sessionId = response.getSessionId();

			if (sessionId != null && !sessions.contains(sessionId)) {
				synchronized (sessions) {
					if (!sessions.contains(sessionId)) {
						sessions.add(sessionId);
					}
				}
			}

			return response;

		} catch (TException e) {
			throw new ThriftTransportException(
					"Error sending request to the remote server", e);
		} finally {
			clientPool.release(client);
		}
	}

	protected void internalSendRequestThrift(Request<Object> request,
			final Class<JsonElement> resultClass,
			final Continuation<Response<JsonElement>> continuation) {

		log.debug("Req-> {}", request);

		// TODO Remove this hack -----------------------
		if (request.getMethod().equals("subscribe")) {
			JsonObject params = (JsonObject) request.getParams();
			params.addProperty("ip", localHandlerAddress.getHostString());
			params.addProperty("port",
					Integer.valueOf(localHandlerAddress.getPort()));
		}
		// ---------------------------------------------

		sendRequest(request, resultClass, continuation, true);
	}

	private void sendRequest(final Request<Object> request,
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
								String response = thriftResponse.getResult();

								log.debug("<-Res {}", response.trim());

								continuation.onSuccess(JsonUtils
										.fromJsonResponse(response, resultClass));

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

		synchronized (keepAliveThread) {
			stopKeepAlive = true;
		}
		keepAliveThread.interrupt();
	}
}
