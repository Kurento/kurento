/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.kmf.jsonrpcconnector.internal.client;

import static com.kurento.kmf.jsonrpcconnector.JsonUtils.INJECT_SESSION_ID;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.kurento.kmf.jsonrpcconnector.Session;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kmf.jsonrpcconnector.client.RequestAlreadyRespondedException;
import com.kurento.kmf.jsonrpcconnector.internal.message.Message;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;
import com.kurento.kmf.jsonrpcconnector.internal.message.ResponseError;

public class TransactionImpl implements Transaction {

	public interface ResponseSender {
		void sendResponse(Message message) throws IOException;
	}

	private final Session session;
	private boolean async;
	private final AtomicBoolean responded = new AtomicBoolean(false);
	private final ResponseSender responseSender;
	private final Request<?> request;

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

			Response<Object> response = new Response<>(request.getId(), result);

			if (INJECT_SESSION_ID) {
				response.setSessionId(session.getSessionId());
			}

			responseSender.sendResponse(response);

		} else {
			throw new RequestAlreadyRespondedException(
					"This request has already been responded");
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

		responseSender.sendResponse(new Response<>(request.getId(),
				new ResponseError(code, message, data)));
	}

	@Override
	public void sendError(Exception e) throws IOException {

		ResponseError error = ResponseError.newFromException(e);
		responseSender.sendResponse(new Response<>(request.getId(), error));

	}

	@Override
	public boolean isNotification() {
		return request.getId() == null;
	}
}
