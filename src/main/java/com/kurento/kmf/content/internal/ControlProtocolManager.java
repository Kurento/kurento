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
package com.kurento.kmf.content.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.google.gson.Gson;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcResponse;

/**
 * 
 * This class handles the JSON-based representations for information exchange
 * (media negotiation).
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
public class ControlProtocolManager {

	private static final Logger log = LoggerFactory
			.getLogger(ControlProtocolManager.class);

	private static final int BUFF = 4096;

	/**
	 * Encodings accepted in JSON (UTF-8, UTF-16BE/LE, UTF-32BE/LE).
	 */
	private static final String UTF8 = "UTF-8";
	private static final String UTF16BE = "UTF-16BE";
	private static final String UTF16LE = "UTF-16LE";
	private static final String UTF32BE = "UTF-32BE";
	private static final String UTF32LE = "UTF-32LE";

	private Gson gson;

	/**
	 * Default constructor; it creates the Gson (Google JSON API) instance.
	 */
	public ControlProtocolManager() {
		gson = new Gson();
	}

	/**
	 * Receiver method for JSON throw a request.
	 * 
	 * @param asyncCtx
	 *            Asynchronous context
	 * @return Received JSON encapsulated as a Java class
	 */
	public JsonRpcRequest receiveJsonRequest(AsyncContext asyncCtx) {
		HttpServletRequest request = (HttpServletRequest) asyncCtx.getRequest();

		// Received character encoding should be UTF-8. In order to check this,
		// the method detectJsonEncoding will be used. Before that, the
		// InputStream read from request.getInputStream() should be cloned
		// (using a ByteArrayOutputStream) to be used on detectJsonEncoding and
		// then for reading the JSON message

		try {
			InputStream inputStream = request.getInputStream();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[BUFF];
			int len;
			while ((len = inputStream.read(buffer)) > -1) {
				baos.write(buffer, 0, len);
			}
			baos.flush();

			String encoding = detectJsonEncoding(new ByteArrayInputStream(
					baos.toByteArray()));
			log.debug("Detected JSON encoding: " + encoding);
			if (encoding == null) {
				throw new KurentoMediaFrameworkException(
						"Invalid or unsupported charset encondig in received JSON request",
						10018);
			}

			InputStreamReader isr = new InputStreamReader(
					new ByteArrayInputStream(baos.toByteArray()), encoding);

			JsonRpcRequest jsonRequest = gson.fromJson(isr,
					JsonRpcRequest.class);
			Assert.notNull(jsonRequest.getMethod());
			log.info("Received JsonRpc request ...\n " + jsonRequest.toString());
			return jsonRequest;
		} catch (IOException e) {
			// TODO: trace this exception and double check appropriate JsonRpc
			// answer is sent
			throw new KurentoMediaFrameworkException(
					"IOException reading JsonRPC request. Cause: "
							+ e.getMessage(), e, 20016);
		}
	}

	/**
	 * Sender method for JSON throw a request.
	 * 
	 * @param asyncCtx
	 *            Asynchronous context
	 * @param message
	 *            JSON message (as a Java class)
	 * @throws IOException
	 *             Exception while parsing operating with asynchronous context
	 */
	public void sendJsonAnswer(AsyncContext asyncCtx, JsonRpcResponse message) {
		internalSendJsonAnswer(asyncCtx, message);
	}

	/**
	 * Sender method for error messages in JSON throw a request.
	 * 
	 * @param asyncCtx
	 *            Asynchronous context
	 * @param message
	 *            JSON error message (as a Java class)
	 * @throws IOException
	 *             Exception while parsing operating with asynchronous context
	 */
	public void sendJsonError(AsyncContext asyncCtx, JsonRpcResponse message) {
		try {
			internalSendJsonAnswer(asyncCtx, message);
		} catch (Throwable e) {
			log.info("Cannot send answer message to destination", e);
		} finally {
			if (asyncCtx != null) {
				asyncCtx.complete();
			}
		}
	}

	/**
	 * Internal implementation for sending JSON (called from sendJsonAnswer and
	 * sendJsonError methods).
	 * 
	 * @param asyncCtx
	 *            Asynchronous context
	 * @param message
	 *            JSON message (as a Java class)
	 * @throws IOException
	 *             Exception while parsing operating with asynchronous context
	 */
	private void internalSendJsonAnswer(AsyncContext asyncCtx,
			JsonRpcResponse message) {
		try {
			if (asyncCtx == null) {
				throw new KurentoMediaFrameworkException("Null asyncCtx found",
						20017);
			}

			synchronized (asyncCtx) {
				if (!asyncCtx.getRequest().isAsyncStarted()) {
					throw new KurentoMediaFrameworkException(
							"Cannot send message in completed asyncCtx", 1); // TODO
				}

				HttpServletResponse response = (HttpServletResponse) asyncCtx
						.getResponse();
				response.setContentType("application/json");
				OutputStreamWriter osw = new OutputStreamWriter(
						response.getOutputStream(), UTF8);
				osw.write(gson.toJson(message));
				osw.flush();
				log.info("Sent JsonRpc answer ...\n" + message);
				asyncCtx.complete();
			}
		} catch (IOException ioe) {
			throw new KurentoMediaFrameworkException(ioe.getMessage(), ioe,
					20018);
		}
	}

	/**
	 * Parses Java class to JSON.
	 * 
	 * @param object
	 *            Generic objetc to be parsed
	 * @return JSON serialization
	 */
	public String toString(Object object) {
		return gson.toJson(object);
	}

	/**
	 * Reads inputStream (from request) and detects incoming JSON encoding.
	 * 
	 * @param inputStream
	 *            Input Stream from request
	 * @return String identifier for detected JSON (UTF8, UTF16LE, ...)
	 * @throws IOException
	 *             Exception while parsing JSON
	 */
	private String detectJsonEncoding(InputStream inputStream)
			throws IOException {
		inputStream.mark(4);
		int mask = 0;
		for (int count = 0; count < 4; count++) {
			int r = inputStream.read();
			if (r == -1) {
				break;
			}
			mask = mask << 1;
			mask |= (r == 0) ? 0 : 1;
		}
		inputStream.reset();
		return match(mask);
	}

	/**
	 * Match recovered mask to String identifier (UTF8, UTF16LE, ...).
	 * 
	 * @param mask
	 *            Mask from detectJsonEncoding method
	 * @return String identifier for the detected JSON encoding
	 */
	private String match(int mask) {
		switch (mask) {
		case 1:
			return UTF32BE;
		case 5:
			return UTF16BE;
		case 8:
			return UTF32LE;
		case 10:
			return UTF16LE;
		case 15:
			return UTF8;
		default:
			return null;
		}
	}
}
