package com.kurento.demo.junit;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ParallelPlayerIT extends BaseArquillianTst {

	private static final int nThreads = 5;

	private String checksum;

	@Before
	public void setUp() throws NoSuchAlgorithmException, IOException {
		InputStream videoToPlay = Thread.currentThread()
				.getContextClassLoader().getResourceAsStream("small.webm");
		checksum = createChecksum(videoToPlay);
	}

	@Test
	public void testParallelPlayRedirect() throws ClientProtocolException,
			IOException, InterruptedException, ExecutionException {
		testParallelPlay("http://localhost:" + getServerPort()
				+ "/content-api-test/player-play-with-redirect", 200,
				"video/webm", false);
	}

	@Ignore
	@Test
	public void testParallelPlayTunnel() throws ClientProtocolException,
			IOException, InterruptedException, ExecutionException {
		testParallelPlay("http://localhost:" + getServerPort()
				+ "/content-api-test/player-play-with-tunnel", 200,
				"video/webm", false);
	}

	@Ignore
	@Test
	public void testParallelRejectRedirect() throws ClientProtocolException,
			IOException, InterruptedException, ExecutionException {
		testParallelPlay("http://localhost:" + getServerPort()
				+ "/content-api-test/player-reject-with-redirect", 407, null,
				false);
	}

	@Ignore
	@Test
	public void testParallelRejectTunnel() throws ClientProtocolException,
			IOException, InterruptedException, ExecutionException {
		testParallelPlay("http://localhost:" + getServerPort()
				+ "/content-api-test/player-reject-with-tunnel", 407, null,
				false);
	}

	@Ignore
	@Test
	public void testParallelInterruptRedirect() throws ClientProtocolException,
			IOException, InterruptedException, ExecutionException {
		testParallelPlay("http://localhost:" + getServerPort()
				+ "/content-api-test/player-play-with-redirect", 200, null,
				true);
	}

	@Ignore
	@Test
	public void testParallelInterruptTunnel() throws ClientProtocolException,
			IOException, InterruptedException, ExecutionException {
		testParallelPlay("http://localhost:" + getServerPort()
				+ "/content-api-test/player-play-with-tunnel", 200, null, true);
	}

	private void testParallelPlay(String url, int statusCode,
			String contentType, boolean interrupt)
			throws ClientProtocolException, IOException, InterruptedException,
			ExecutionException {
		ExecutorService execute = Executors.newFixedThreadPool(nThreads);
		Collection<Future<?>> futures = new LinkedList<Future<?>>();

		// Perform nThreads calls
		for (int i = 0; i < nThreads; i++) {
			futures.add(execute.submit(new ParallelTest(url, statusCode,
					contentType, interrupt)));
		}

		// Wait for all threads to be terminated
		for (Future<?> future : futures) {
			future.get();
		}
	}

	class ParallelTest implements Runnable {
		private String url;
		private int statusCode;
		private String contentType;
		private boolean interrupt;

		public ParallelTest(String url, int statusCode, String contentType,
				boolean interrupt) {
			this.url = url;
			this.statusCode = statusCode;
			this.contentType = contentType;
			this.interrupt = interrupt;
		}

		public void run() {
			try {
				HttpClient client = new DefaultHttpClient();
				HttpGet httpGet = new HttpGet(url);
				HttpResponse response = client.execute(httpGet);
				HttpEntity resEntity = response.getEntity();

				if (interrupt) {
					resEntity.getContent().close();
				} else {
					if (contentType == null) {
						// Rejected
						EntityUtils.consume(resEntity);
					} else {
						// createChecksum reads inputStream to its end and
						// closes it, i.e. it is equivalent to
						// EntityUtils.consume(resEntity);

						// TODO: Uncomment these lines to check file integrity
						// InputStream inputStream = resEntity.getContent();
						// String newChecksum = createChecksum(inputStream);
						// Assert.assertEquals("Uploaded file integrity failed ",
						// checksum, newChecksum);
					}
				}

				final int responseStatusCode = response.getStatusLine()
						.getStatusCode();
				log.debug("ReasonPhrase "
						+ response.getStatusLine().getReasonPhrase());
				log.debug("statusCode " + statusCode);
				Assert.assertEquals("HTTP response status code must be "
						+ statusCode, statusCode, responseStatusCode);

				if (contentType != null) {
					Header responseContentType = resEntity.getContentType();
					log.debug("contentType " + responseContentType.getValue());
					Assert.assertEquals(
							"Content-Type in response header must be "
									+ contentType, contentType,
							responseContentType.getValue());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
