package com.kurento.kmf.jsonrpcconnector.client;

import static com.kurento.kmf.jsonrpcconnector.JsonUtils.fromJsonResponse;
import static com.kurento.kmf.jsonrpcconnector.JsonUtils.toJson;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.kurento.kmf.jsonrpcconnector.JsonUtils;
import com.kurento.kmf.jsonrpcconnector.Session;
import com.kurento.kmf.jsonrpcconnector.internal.HttpResponseSender;
import com.kurento.kmf.jsonrpcconnector.internal.JsonRpcRequestSenderHelper;
import com.kurento.kmf.jsonrpcconnector.internal.client.ClientSession;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;
import com.kurento.kmf.jsonrpcconnector.internal.message.ResponseError;

public class JsonRpcClientHttp extends JsonRpcClient {

	private Logger log = LoggerFactory.getLogger(JsonRpcClient.class);

	private Thread longPoolingThread;
	private String url;

	private Session session;
	private HttpResponseSender rs;

	public JsonRpcClientHttp(String url) {
		this.url = url;
		this.rs = new HttpResponseSender();
		this.rsHelper = new JsonRpcRequestSenderHelper() {
			@Override
			public <P, R> Response<R> internalSendRequest(Request<P> request,
					Class<R> resultClass) throws IOException {
				return internalSendRequestHttp(request, resultClass);
			}

			@Override
			protected void internalSendRequest(Request<Object> request,
					Class<JsonElement> class1,
					Continuation<Response<JsonElement>> continuation) {
				throw new UnsupportedOperationException(
						"Async client int local is unavailable");
			}
		};
	}

	private void updateSession(Response<?> response) {

		String sessionId = response.getSessionId();

		rsHelper.setSessionId(sessionId);

		if (session == null) {
			session = new ClientSession(sessionId, registerInfo, this);

			handlerManager.afterConnectionEstablished(session);

			startPooling();
		}
	}

	private void startPooling() {
		this.longPoolingThread = new Thread() {
			@Override
			public void run() {
				longPooling();
			}
		};

		this.longPoolingThread.start();
	}

	private void longPooling() {

		while (true) {

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			if (Thread.interrupted()) {
				break;
			}

			try {

				JsonElement requestsListJsonObject = this.sendRequest(
						Request.POLL_METHOD_NAME, rs.getResponseListToSend(),
						JsonElement.class);

				log.info("Response from pool:" + requestsListJsonObject);

				Type collectionType = new TypeToken<List<Request<JsonElement>>>() {
				}.getType();

				List<Request<JsonElement>> requestList = JsonUtils.fromJson(
						requestsListJsonObject, collectionType);

				processServerRequests(requestList);

			} catch (IOException e) {
				// TODO Decide what to do in this case. If the net connection is
				// lost, this will retry indefinitely
				log.error("Exeception when waiting for events (long-pooling). Retry");
			}
		}
	}

	private void processServerRequests(List<Request<JsonElement>> requestList) {
		for (Request<JsonElement> request : requestList) {
			try {
				handlerManager.handleRequest(session, request, rs);
			} catch (IOException e) {
				log.error(
						"Exception while processing request from server to client",
						e);
			}
		}
	}

	private <P, R> Response<R> internalSendRequestHttp(Request<P> request,
			Class<R> resultClass) throws IOException {

		String resultJson = org.apache.http.client.fluent.Request.Post(url)
				.bodyString(toJson(request), ContentType.APPLICATION_JSON)
				.execute().returnContent().asString();

		if (resultJson == null || resultJson.trim().isEmpty()) {
			return new Response<R>(request.getId(), new ResponseError(3,
					"The server send an empty response", null));
		}

		Response<R> response = fromJsonResponse(resultJson, resultClass);

		updateSession(response);

		return response;
	}

	@Override
	public void close() {
		if (this.longPoolingThread != null) {
			log.info("Interrupted!!!!");
			this.longPoolingThread.interrupt();
		}
		handlerManager.afterConnectionClosed(session,
				"Client closed connection");
		session = null;
	}

}
