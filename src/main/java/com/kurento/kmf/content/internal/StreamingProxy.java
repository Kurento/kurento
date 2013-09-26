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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.content.ContentApiConfiguration;

/**
 * Media content can be served from the Media Server directly straight to the
 * client, but it could also be proxied through the Application Server; this
 * class implemented this proxy.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
public class StreamingProxy {

	/**
	 * Logger.
	 */
	private static final Logger log = LoggerFactory
			.getLogger(StreamingProxy.class);

	/**
	 * Autowired configuration.
	 */
	@Autowired
	private ContentApiConfiguration configuration;

	/**
	 * Apache implementation of an HTTP client.
	 */
	private HttpClient httpClient;

	/**
	 * Autowired thread pool.
	 */
	@Autowired
	private ContentApiExecutorService executorService;

	/**
	 * HTTP headers accepted in the request by proxy.
	 */
	private final static String[] ALLOWED_REQUEST_HEADERS = { "accept",
			"accept-charset", "accept-encoding", "accept-language",
			"accept-datetime", "cache-control", "connection", "date", "expect",
			"if-match", "if-modified-since", "if-none-match", "if-range",
			"if-unmodified-since", "max-forwards", "pragma", "range", "te",
			"x-forwarded-for", "via" };

	/**
	 * HTTP headers sent in the response by proxy.
	 */
	private final static String[] ALLOWED_RESPONSES_HEADERS = {
			"Content-Location", "Content-MD5", "ETag", "Last-Modified",
			"Expires", "Content-Encoding", "Content-Range", "Content-Type" };

	/**
	 * Buffer size.
	 */
	private final static int BUFF = 2048;

	/**
	 * It seeks the occurrence of a String within an array.
	 * 
	 * @param strs
	 *            Array
	 * @param str
	 *            String
	 * @return true|false
	 */
	private static boolean contains(String[] strs, String str) {
		if (strs == null || str == null)
			return false;

		for (String element : strs) {
			if (str.equalsIgnoreCase(element))
				return true;
		}
		return false;
	}

	/**
	 * Default constructor.
	 */
	public StreamingProxy() {
	}

	/**
	 * After constructor method; it created the HTTP client using configuration
	 * parameters {@link ContentApiConfiguration}.
	 * 
	 * @see ContentApiConfiguration
	 */
	@PostConstruct
	public void afterPropertiesSet() {
		HttpParams params = new BasicHttpParams();
		params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,
				configuration.getProxyConnectionTimeout());
		params.setParameter(CoreConnectionPNames.SO_TIMEOUT,
				configuration.getProxySocketTimeout());

		// Thread-safe configuration (using PoolingClientConnectionManager)
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory
				.getSocketFactory()));
		schemeRegistry.register(new Scheme("https", 443, SSLSocketFactory
				.getSocketFactory()));
		PoolingClientConnectionManager cm = new PoolingClientConnectionManager(
				schemeRegistry);
		cm.setMaxTotal(configuration.getProxyMaxConnections());
		cm.setDefaultMaxPerRoute(configuration.getProxyMaxConnectionsPerRoute());

		httpClient = new DefaultHttpClient(cm, params);
	}

	/**
	 * It tunnels a request using by means of the thread pool.
	 * 
	 * @param clientSideRequest
	 *            Client request
	 * @param clientSideResponse
	 *            Client response
	 * @param serverSideUrl
	 *            URL which triggers the request
	 * @param streamingProxyListener
	 *            Proxy listener
	 * @return Future object
	 * @throws IOException
	 */
	public Future<?> tunnelTransaction(HttpServletRequest clientSideRequest,
			HttpServletResponse clientSideResponse, String serverSideUrl,
			StreamingProxyListener streamingProxyListener) throws IOException {

		ProxyThread proxyThread = new ProxyThread(clientSideRequest,
				clientSideResponse, serverSideUrl, streamingProxyListener);
		return executorService.getExecutor().submit(proxyThread);
	}

	/**
	 * Anonymous class implementing the threads of the pool for the streaming
	 * proxy.
	 * 
	 * @author Luis López (llopez@gsyc.es)
	 * @author Boni García (bgarcia@gsyc.es)
	 * @version 1.0.0
	 * 
	 */
	class ProxyThread implements RejectableRunnable {

		/**
		 * Client HTTP request.
		 */
		private HttpServletRequest clientSideRequest;

		/**
		 * Client HTTP response.
		 */
		private HttpServletResponse clientSideResponse;

		/**
		 * Media URL.
		 */
		private String serverSideUrl;

		/**
		 * Event listener for proxy actions (error, success).
		 */
		private StreamingProxyListener streamingProxyListener;

		/**
		 * Parameterized constructor.
		 * 
		 * @param clientSideRequest
		 *            Client request
		 * @param clientSideResponse
		 *            Client response
		 * @param serverSideUrl
		 *            URL which triggers the request
		 * @param streamingProxyListener
		 *            Proxy listener
		 */
		public ProxyThread(HttpServletRequest clientSideRequest,
				HttpServletResponse clientSideResponse, String serverSideUrl,
				StreamingProxyListener streamingProxyListener) {
			this.clientSideRequest = clientSideRequest;
			this.clientSideResponse = clientSideResponse;
			this.serverSideUrl = serverSideUrl;
			this.streamingProxyListener = streamingProxyListener;
		}

		/**
		 * Thread runner. It does not raises any exception, but it raises events
		 * for Proxy Listener (onProxySuccess, onProxyError).
		 */
		@Override
		public void run() {
			HttpRequestBase tunnelRequest = null;
			HttpEntity tunnelResponseEntity = null;

			try {
				Enumeration<String> clientSideHeaders = clientSideRequest
						.getHeaderNames();
				List<BasicHeader> tunneledHeaders = new ArrayList<BasicHeader>();
				while (clientSideHeaders.hasMoreElements()) {
					String headerName = clientSideHeaders.nextElement()
							.toLowerCase();
					if (contains(ALLOWED_REQUEST_HEADERS, headerName)) {
						tunneledHeaders.add(new BasicHeader(headerName,
								clientSideRequest.getHeader(headerName)));
					}
				}

				String method = clientSideRequest.getMethod();
				if (method.equalsIgnoreCase("GET")) {
					tunnelRequest = new HttpGet(serverSideUrl);
				} else if (method.equalsIgnoreCase("POST")) {
					tunnelRequest = new HttpPost(serverSideUrl);
					InputStreamEntity postEntity = new InputStreamEntity(
							clientSideRequest.getInputStream(),
							clientSideRequest.getContentLength(),
							ContentType.create(clientSideRequest
									.getContentType()));
					((HttpPost) tunnelRequest).setEntity(postEntity);
				} else {
					throw new IOException("Method " + method
							+ " not supported on internal tunneling proxy");
				}

				for (BasicHeader header : tunneledHeaders) {
					tunnelRequest.addHeader(header);
				}

				// TODO: Does this throws interrupted exception? Does this
				// recognize thread interruption? Tests must be made
				HttpResponse tunnelResponse = httpClient.execute(tunnelRequest);

				clientSideResponse.setStatus(tunnelResponse.getStatusLine()
						.getStatusCode());

				for (Header header : tunnelResponse.getAllHeaders()) {
					if (contains(ALLOWED_RESPONSES_HEADERS, header.getName())) {
						clientSideResponse.setHeader(header.getName(),
								header.getValue());
					}
				}

				tunnelResponseEntity = tunnelResponse.getEntity();
				if (tunnelResponseEntity != null) {
					byte[] block = new byte[BUFF];

					while (true) {
						if (Thread.currentThread().isInterrupted()) {
							throw new InterruptedException();
						}

						int len = tunnelResponseEntity.getContent().read(block);
						if (len < 0) {
							break;
						}

						clientSideResponse.getOutputStream().write(block, 0,
								len);
						// TODO: browser stopping a video generates an exception
						// here. Are we sure everything is cleanly closed on the
						// management of the exception
						clientSideResponse.flushBuffer();
					}
				}
				streamingProxyListener.onProxySuccess();

			} catch (IOException e) {
				log.error("Code 20019. Exception in streaming proxy", e);
				streamingProxyListener.onProxyError(e.getMessage(), 20019);
			} catch (InterruptedException e) {
				log.error("Code 20025. Exception in streaming proxy", e);
				streamingProxyListener.onProxyError(e.getMessage(), 20025);
			} finally {
				if (tunnelResponseEntity != null) {
					try {
						EntityUtils.consume(tunnelResponseEntity);
					} catch (IOException e) {
						log.info("Error consuming tunnel response entity", e);
					}
				}
				if (tunnelRequest != null) {
					tunnelRequest.releaseConnection();
				}
			}
		}

		/**
		 * Execution rejected event.
		 */
		@Override
		public void onExecutionRejected() {
			streamingProxyListener.onProxyError(
					"Servler overloaded. Try again in a few minutes", 20011);
		}
	}
}
