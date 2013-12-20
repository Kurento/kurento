package com.kurento.kmf.jsonrpcconnector.internal.server;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.kurento.kmf.jsonrpcconnector.Session;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kmf.jsonrpcconnector.internal.message.Message;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;
import com.kurento.kmf.jsonrpcconnector.internal.message.ResponseError;
import com.kurento.kmf.jsonrpcconnector.internal.server.config.JsonRpcConfiguration;

public class TransactionImpl implements Transaction {

	public interface ResponseSender {
		void sendResponse(Message message) throws IOException;
	}

	private Session session;
	private boolean async = false;
	private AtomicBoolean responded = new AtomicBoolean(false);
	private ResponseSender responseSender;
	private Request<?> request;

	public TransactionImpl(Session session, Request<?> request,
			ResponseSender responseSender) {
		super();
		this.session = session;
		this.responseSender = responseSender;
		this.request = request;
	}

	@Override
	public void sendResponse(Object result) throws IOException {

		boolean notResponded = setRespondedIfNot();

		if (notResponded) {

			Response<Object> response = new Response<Object>(request.getId(),
					result);

			if (JsonRpcConfiguration.INJECT_SESSION_ID) {
				response.setSessionId(session.getSessionId());
			}

			responseSender.sendResponse(response);

		} else {
			throw new RuntimeException("This request has been yet responded");
		}
	}

	@Override
	public Session getSession() {
		return session;
	}

	@Override
	public void startAsync() {
		async = true;
	}

	public boolean isAsync() {
		return async;
	}

	public boolean setRespondedIfNot() {
		return responded.compareAndSet(false, true);
	}

	@Override
	public void sendError(int code, String message, String data)
			throws IOException {

		responseSender.sendResponse(new Response<Object>(request.getId(),
				new ResponseError(code, message, data)));
	}

	@Override
	public void sendError(Exception e) throws IOException {

		ResponseError error = ResponseError.newFromException(e);
		responseSender
				.sendResponse(new Response<Object>(request.getId(), error));

	}

	@Override
	public boolean isNotification() {
		return request.getId() == null;
	}
}
