package com.kurento.kmf.content.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

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

public class StreamingProxy {

	private static final Logger log = LoggerFactory
			.getLogger(StreamingProxy.class);

	@Autowired
	private ContentApiConfiguration configuration;

	private HttpClient httpClient;

	private final static String[] ALLOWED_REQUEST_HEADERS = { "accept",
			"accept-charset", "accept-encoding", "accept-language",
			"accept-datetime", "cache-control", "connection", "date", "expect",
			"if-match", "if-modified-since", "if-none-match", "if-range",
			"if-unmodified-since", "max-forwards", "pragma", "range", "te",
			"x-forwarded-for", "via" };

	private final static String[] ALLOWED_RESPONSES_HEADERS = {
			"Content-Location", "Content-MD5", "ETag", "Last-Modified",
			"Expires", "Content-Encoding", "Content-Range", "Content-Type" };

	private final static int BUFF = 2048;

	private static boolean contains(String[] strs, String str) {
		if (strs == null || str == null)
			return false;

		for (String element : strs) {
			if (str.equalsIgnoreCase(element))
				return true;
		}
		return false;
	}

	public StreamingProxy() {
	}

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

	public void tunnelTransaction(HttpServletRequest clientSideRequest,
			HttpServletResponse clientSideResponse, String serverSideUrl)
			throws IOException {

		Enumeration<String> clientSideHeaders = clientSideRequest
				.getHeaderNames();
		List<BasicHeader> tunneledHeaders = new ArrayList<BasicHeader>();
		while (clientSideHeaders.hasMoreElements()) {
			String headerName = clientSideHeaders.nextElement().toLowerCase();
			if (contains(ALLOWED_REQUEST_HEADERS, headerName)) {
				tunneledHeaders.add(new BasicHeader(headerName,
						clientSideRequest.getHeader(headerName)));
			}
		}

		HttpRequestBase tunnelRequest = null;
		HttpEntity tunnelResponseEntity = null;
		try {
			String method = clientSideRequest.getMethod();
			if (method.equalsIgnoreCase("GET")) {
				tunnelRequest = new HttpGet(serverSideUrl);
			} else if (method.equalsIgnoreCase("POST")) {
				tunnelRequest = new HttpPost(serverSideUrl);
				InputStreamEntity postEntity = new InputStreamEntity(
						clientSideRequest.getInputStream(),
						clientSideRequest.getContentLength(),
						ContentType.create(clientSideRequest.getContentType()));
				((HttpPost) tunnelRequest).setEntity(postEntity);
			} else {
				throw new IOException("Method " + method
						+ " not supported on internal tunneling proxy");
			}

			for (BasicHeader header : tunneledHeaders) {
				tunnelRequest.addHeader(header);
			}

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
					int len = tunnelResponseEntity.getContent().read(block);
					if (len < 0) {
						break;
					}

					clientSideResponse.getOutputStream().write(block, 0, len);
					// TODO: browser stopping a video generates an exception
					// here. Are we sure everything is cleanly closed on the
					// management of the exception
					clientSideResponse.flushBuffer();
				}
			}

		} finally {
			if (tunnelResponseEntity != null) {
				EntityUtils.consume(tunnelResponseEntity);
			}
			if (tunnelRequest != null) {
				tunnelRequest.releaseConnection();
			}
		}
	}
}
