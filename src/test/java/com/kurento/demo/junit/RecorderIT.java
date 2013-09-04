package com.kurento.demo.junit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@Ignore
@RunWith(Arquillian.class)
public class RecorderIT extends BaseArquillianTst {

	private File fileToUpload;

	private File fileUploaded;

	private String checksum;

	@Before
	public void setUp() throws IOException, NoSuchAlgorithmException {
		// Sample file from resources
		InputStream input = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("small.webm");
		// If we would like to open a sample file from the web:
		// InputStream input = new URL("...").openStream();

		// Temporal file
		fileToUpload = new File("fileToUpload");
		OutputStream output = new FileOutputStream(fileToUpload);
		byte[] buf = new byte[BUFF];
		int len;
		while ((len = input.read(buf)) > 0) {
			output.write(buf, 0, len);
		}
		output.close();
		input.close();
		checksum = createChecksum(fileToUpload);
	}

	@Test
	public void testRecordTunnel() throws ClientProtocolException, IOException,
			NoSuchAlgorithmException {
		testRecord("http://localhost:" + getServerPort()
				+ "/content-demo/recorder-with-tunnel", 200,
				"myfile-with-tunnel", "application/octet-stream", false);
	}

	@Test
	public void testRecordRedirect() throws ClientProtocolException,
			IOException, NoSuchAlgorithmException {
		testRecord("http://localhost:" + getServerPort()
				+ "/content-demo/recorder-with-redirect", 200,
				"myfile-with-redirect", "application/octet-stream", false);
	}

	@Test
	public void testRecordTunnelReject() throws ClientProtocolException,
			IOException, NoSuchAlgorithmException {
		testRecord("http://localhost:" + getServerPort()
				+ "/content-demo/recorder-with-tunnel-and-reject", 407,
				"myfile-with-tunnel", null, false);
	}

	@Test
	public void testRecordRedirectReject() throws ClientProtocolException,
			IOException, NoSuchAlgorithmException {
		testRecord("http://localhost:" + getServerPort()
				+ "/content-demo/recorder-with-redirect-and-reject", 407,
				"myfile-with-redirect", null, false);
	}

	@Test
	public void testInterruptTunnel() throws ClientProtocolException,
			IOException, NoSuchAlgorithmException {
		testRecord("http://localhost:" + getServerPort()
				+ "/content-demo/recorder-with-tunnel", 200,
				"myfile-with-tunnel", null, true);
	}

	@Test
	public void testnterruptRedirect() throws ClientProtocolException,
			IOException, NoSuchAlgorithmException {
		testRecord("http://localhost:" + getServerPort()
				+ "/content-demo/recorder-with-redirect", 200,
				"myfile-with-redirect", null, true);
	}

	private void testRecord(String url, int statusCode,
			String fileUploadedName, String contentType, boolean interrupt)
			throws ClientProtocolException, IOException,
			NoSuchAlgorithmException {
		HttpClient client = new DefaultHttpClient();

		// Set automatic redirect for HTTP 307 (SC_TEMPORARY_REDIRECT)
		((DefaultHttpClient) client)
				.setRedirectStrategy(new DefaultRedirectStrategy() {
					public boolean isRedirected(HttpRequest request,
							HttpResponse response, HttpContext context)
							throws ProtocolException {
						boolean isRedirect = false;
						isRedirect = super.isRedirected(request, response,
								context);
						if (!isRedirect) {
							int responseCode = response.getStatusLine()
									.getStatusCode();
							if (responseCode == HttpStatus.SC_TEMPORARY_REDIRECT) {
								return true;
							}
						}
						return isRedirect;
					}
				});

		HttpPost httpPost = new HttpPost(url);
		MultipartEntity entity = new MultipartEntity(
				HttpMultipartMode.BROWSER_COMPATIBLE);
		FileBody fileBody = new FileBody(fileToUpload);
		entity.addPart(fileUploadedName, fileBody);

		httpPost.setEntity(entity);
		HttpResponse response = client.execute(httpPost);
		HttpEntity resEntity = response.getEntity();

		// TODO HttpEndPointConfiguration not available
		// HttpEndPointConfiguration config = new HttpEndPointConfiguration();
		// fileUploaded = new File(config.getRecorderBaseDir() + File.separator
		// + fileUploadedName);

		if (interrupt) {
			resEntity.getContent().close();
		} else if (contentType != null) {
			// // Assert that file has been uploaded
			// Assert.assertTrue("File has not been uploaded correctly",
			// fileUploaded.exists());
			//
			// // Assert that uploaded file has integrity
			// Assert.assertTrue("Uploaded file integrity failed",
			// createChecksum(fileUploaded).equals(checksum));
		}

		// Assert status code in response
		final int responseStatusCode = response.getStatusLine().getStatusCode();
		Assert.assertEquals("HTTP response status code must be " + statusCode,
				statusCode, responseStatusCode);

		// Assert Content-Type in response
		if (contentType != null && resEntity.getContentType() != null) {
			Header responseContentType = resEntity.getContentType();
			log.debug("contentType " + responseContentType.getValue());
			Assert.assertEquals("Content-Type in response header must be "
					+ contentType, contentType, responseContentType.getValue());
		}
	}

	@After
	public void close() {
		// Delete temporal files
		deleteFile(fileToUpload);
		deleteFile(fileUploaded);
		// HttpEndPointConfiguration config = new HttpEndPointConfiguration();
		// deleteFile(new File(config.getRecorderBaseDir()));
	}

	private void deleteFile(File file) {
		if (file.exists()) {
			log.debug("Deleting file " + file.getName() + " : " + file.delete());
		}
	}
}
