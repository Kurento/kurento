/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kurento.jsonrpc.test.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonObject;

import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.test.base.DemoBean;

public class EchoJsonRpcHandler extends DefaultJsonRpcHandler<JsonObject> {

	private static Logger log = LoggerFactory
			.getLogger(EchoJsonRpcHandler.class);

	@Autowired
	DemoBean demoBean;

	@Override
	public void handleRequest(Transaction transaction,
			Request<JsonObject> request) throws Exception {

		if (demoBean == null) {
			throw new RuntimeException("Not autowired dependencies");
		}
		log.info("Request id:" + request.getId());
		log.info("Request method:" + request.getMethod());
		log.info("Request params:" + request.getParams());

		transaction.sendResponse(request.getParams());

	}

}
