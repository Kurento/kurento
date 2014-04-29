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
package com.kurento.kmf.jsonrpcconnector.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.kurento.kmf.jsonrpcconnector.internal.client.TransactionImpl.ResponseSender;
import com.kurento.kmf.jsonrpcconnector.internal.message.Message;
import com.kurento.kmf.jsonrpcconnector.internal.message.Response;

public class HttpResponseSender implements ResponseSender {

	private List<Response<Object>> responses = new ArrayList<>();

	public synchronized List<Response<Object>> getResponseListToSend() {
		List<Response<Object>> returnResponses = responses;
		responses = new ArrayList<>();
		return returnResponses;
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized void sendResponse(Message message) throws IOException {
		responses.add((Response<Object>) message);
	}
}
