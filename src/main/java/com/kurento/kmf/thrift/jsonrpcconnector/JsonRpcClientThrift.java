package com.kurento.kmf.thrift.jsonrpcconnector;

import static com.kurento.kmf.jsonrpcconnector.JsonUtils.fromJsonRequest;

import java.io.IOException;
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
import com.kurento.kmf.jsonrpcconnector.client.Continuation;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.internal.JsonRpcRequestSenderHelper;
import com.kurento.kmf.jsonrpcconnector.internal.message.Message;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;
import com.kurento.kmf.jsonrpcconnector.internal.server.TransactionImpl.ResponseSender;
import com.kurento.kmf.thrift.ThriftServer;
import com.kurento.kmf.thrift.internal.ThriftInterfaceExecutorService;
import com.kurento.kmf.thrift.pool.MediaServerClientPoolService;
import com.kurento.kms.thrift.api.KmsMediaHandlerService;
import com.kurento.kms.thrift.api.KmsMediaServerService;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.invokeJsonRpc_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.Client;

public class JsonRpcClientThrift extends JsonRpcClient {

	private static final Logger LOG = LoggerFactory
			.getLogger(JsonRpcClientThrift.class);

	public static final int KEEP_ALIVE_TIME = 120000;

	private MediaServerClientPoolService clientPool;

	private final ResponseSender dummyResponseSenderForEvents = new ResponseSender() {
		@Override
		public void sendResponse(Message message) throws IOException {
			LOG.warn("The thrift client is trying to send "
					+ " the response '"
					+ message
					+ "' for a request from server. But with Thrift it is not possible");
		}
	};

	private ThriftServer server;
	private boolean stopKeepAlive = false;
	private final Set<String> sessions = new HashSet<String>();

	private InetSocketAddress localHandlerAddress;

	private final Thread keepAliveThread = new Thread(new Runnable() {

		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(KEEP_ALIVE_TIME);
				} catch (InterruptedException e) {

				}

				synchronized (keepAliveThread) {
					if (stopKeepAlive)
						return;
				}

				Set<String> copiedSessions = new HashSet<String>();

				synchronized (sessions) {
					copiedSessions.addAll(sessions);
				}

				/* sendKeepAlives */
				for (String session : copiedSessions) {
					int id = new Random().nextInt();
					Request<Void> request = new Request<Void>(session, id,
							"keepAlive", null);

					LOG.info("Sending keep alive for session: {}", session);
					Response<Void> response = internalSendRequestThrift(
							request, Void.class);
					if (response.isError()) {
						LOG.error("Error on session {} keep alive, removing",
								session);
						synchronized (sessions) {
							sessions.remove(session);
						}
					}
				}
			}
		}
	});

	public JsonRpcClientThrift(MediaServerClientPoolService clientPool,
			ThriftInterfaceExecutorService executorService,
			InetSocketAddress localHandlerAddress) {

		this.clientPool = clientPool;

		this.localHandlerAddress = localHandlerAddress;

		this.rsHelper = new JsonRpcRequestSenderHelper() {
			@Override
			public <P, R> Response<R> internalSendRequest(Request<P> request,
					Class<R> resultClass) throws IOException {
				return internalSendRequestThrift(request, resultClass);
			}

			@Override
			protected void internalSendRequest(Request<Object> request,
					Class<JsonElement> resultClass,
					Continuation<Response<JsonElement>> continuation) {
				internalSendRequestThrift(request, resultClass, continuation);
			}
		};

		final KmsMediaHandlerService.Processor<KmsMediaHandlerService.Iface> clientProcessor = new KmsMediaHandlerService.Processor<KmsMediaHandlerService.Iface>(
				new KmsMediaHandlerService.Iface() {
					@Override
					public void eventJsonRpc(String request) throws TException {

						try {

							LOG.info("[Client] Request Received: " + request);

							JsonObject message = JsonUtils.fromJson(request,
									JsonObject.class);

							handleRequestFromServer(message);

						} catch (Exception e) {
							LOG.error(
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

	public <P, R> Response<R> internalSendRequestThrift(Request<P> request,
			Class<R> resultClass) {

		Client client = clientPool.acquireSync();

		try {

			LOG.info("[Client] Request sent: " + request);

			// TODO Remove this hack -----------------------
			if (request.getMethod().equals("subscribe")) {
				JsonObject params = (JsonObject) request.getParams();
				params.addProperty("ip", localHandlerAddress.getHostName());
				params.addProperty("port", localHandlerAddress.getPort());
			}
			// ---------------------------------------------

			String responseStr = client.invokeJsonRpc(request.toString());

			LOG.info("[Client] Response received: " + responseStr);

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
			throw new RuntimeException(
					"Exception while invoking request to server", e);
		} finally {
			clientPool.release(client);
		}
	}

	protected void internalSendRequestThrift(Request<Object> request,
			final Class<JsonElement> resultClass,
			final Continuation<Response<JsonElement>> continuation) {

		final AsyncClient client = clientPool.acquireAsync();

		LOG.info("[Client] Request sent: " + request);

		// TODO Remove this hack -----------------------
		if (request.getMethod().equals("subscribe")) {
			JsonObject params = (JsonObject) request.getParams();
			params.addProperty("ip", localHandlerAddress.getHostName());
			params.addProperty("port", localHandlerAddress.getPort());
		}
		// ---------------------------------------------

		try {
			client.invokeJsonRpc(
					request.toString(),
					new AsyncMethodCallback<KmsMediaServerService.AsyncClient.invokeJsonRpc_call>() {

						@Override
						public void onError(Exception exception) {
							// TODO Auto-generated method stub
							continuation.onError(exception);
							clientPool.release(client);
						}

						@Override
						public void onComplete(invokeJsonRpc_call thriftResponse) {

							try {
								String response = thriftResponse.getResult();
								LOG.info("[Client] Response received: "
										+ response);

								continuation.onSuccess(JsonUtils
										.fromJsonResponse(response, resultClass));

							} catch (TException e) {
								continuation.onError(e);
							}

							clientPool.release(client);
						}

					});
		} catch (TException e) {
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
