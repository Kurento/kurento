package com.kurento.demo.junit;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

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
public class PlayerIT extends BaseArquillianTst {

	private String checksum;

	@Before
	public void setUp() throws NoSuchAlgorithmException, IOException {
		InputStream videoToPlay = Thread.currentThread()
				.getContextClassLoader().getResourceAsStream("small.webm");
		checksum = createChecksum(videoToPlay);
	}

	@Test
	public void testPlayRedirect() throws ClientProtocolException, IOException,
			NoSuchAlgorithmException {
		testPlay("http://localhost:" + getServerPort()
				+ "/content-api-test/player-play-with-redirect", 200,
				"video/webm", false);
	}

	@Ignore
	@Test
	public void testPlayTunnel() throws ClientProtocolException, IOException,
			NoSuchAlgorithmException {
		testPlay("http://localhost:" + getServerPort()
				+ "/content-api-test/player-play-with-tunnel", 200,
				"video/webm", false);
	}

	@Ignore
	@Test
	public void testRejectRedirect() throws ClientProtocolException,
			IOException, NoSuchAlgorithmException {
		testPlay("http://localhost:" + getServerPort()
				+ "/content-api-test/player-reject-with-redirect", 407, null,
				false);
	}

	@Ignore
	@Test
	public void testRejectTunnel() throws ClientProtocolException, IOException,
			NoSuchAlgorithmException {
		testPlay("http://localhost:" + getServerPort()
				+ "/content-api-test/player-reject-with-tunnel", 407, null,
				false);
	}

	@Ignore
	@Test
	public void testInterruptRedirect() throws ClientProtocolException,
			IOException, NoSuchAlgorithmException {
		testPlay("http://localhost:" + getServerPort()
				+ "/content-api-test/player-play-with-redirect", 200, null,
				true);
	}

	@Ignore
	@Test
	public void testInterruptTunnel() throws ClientProtocolException,
			IOException, NoSuchAlgorithmException {
		testPlay("http://localhost:" + getServerPort()
				+ "/content-api-test/player-play-with-tunnel", 200, null, true);
	}

	private void testPlay(String url, int statusCode, String contentType,
			boolean interrupt) throws ClientProtocolException, IOException,
			NoSuchAlgorithmException {
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
				// createChecksum reads inputStream to its end and closes it,
				// i.e. it is equivalent to EntityUtils.consume(resEntity);

				// TODO: Uncomment these lines to check file integrity
				// InputStream inputStream = resEntity.getContent();
				// String newChecksum = createChecksum(inputStream);
				// Assert.assertEquals("Uploaded file integrity failed ",
				// checksum, newChecksum);
			}
		}

		final int responseStatusCode = response.getStatusLine().getStatusCode();

		log.info("ReasonPhrase " + response.getStatusLine().getReasonPhrase());
		log.info("statusCode " + responseStatusCode);
		Assert.assertEquals("HTTP response status code must be " + statusCode,
				statusCode, responseStatusCode);

		if (contentType != null) {
			Header responseContentType = resEntity.getContentType();
			log.info("contentType " + responseContentType.getValue());
			Assert.assertEquals("Content-Type in response header must be "
					+ contentType, contentType, responseContentType.getValue());
		}
	}
}
