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
package com.kurento.demo.junit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
public class ParallelRecorderIT extends BaseArquillianTst {

	private static final int nThreads = 5;

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
	public void testParallelRecordTunnel() throws ClientProtocolException,
			IOException, NoSuchAlgorithmException, InterruptedException,
			ExecutionException {
		testParallelRecord("http://localhost:" + getServerPort()
				+ "/content-demo/recorder-with-tunnel", 200,
				"myfile-with-tunnel", "application/octet-stream", false);
	}

	@Test
	public void testParallelRecordRedirect() throws ClientProtocolException,
			IOException, NoSuchAlgorithmException, InterruptedException,
			ExecutionException {
		testParallelRecord("http://localhost:" + getServerPort()
				+ "/content-demo/recorder-with-redirect", 200,
				"myfile-with-redirect", "application/octet-stream", false);
	}

	@Test
	public void testParallelRecordTunnelReject()
			throws ClientProtocolException, IOException,
			NoSuchAlgorithmException, InterruptedException, ExecutionException {
		testParallelRecord("http://localhost:" + getServerPort()
				+ "/content-demo/recorder-with-tunnel-and-reject", 407,
				"myfile-with-tunnel", null, false);
	}

	@Test
	public void testParallelRecordRedirectReject()
			throws ClientProtocolException, IOException, InterruptedException,
			ExecutionException {
		testParallelRecord("http://localhost:" + getServerPort()
				+ "/content-demo/recorder-with-redirect-and-reject", 407,
				"myfile-with-redirect", null, false);
	}

	@Test
	public void testParallelRecordInterruptTunnel()
			throws ClientProtocolException, IOException,
			NoSuchAlgorithmException, InterruptedException, ExecutionException {
		testParallelRecord("http://localhost:" + getServerPort()
				+ "/content-demo/recorder-with-tunnel", 200,
				"myfile-with-tunnel", null, true);
	}

	@Test
	public void testParallelRecordInterruptRedirec()
			throws ClientProtocolException, IOException,
			NoSuchAlgorithmException, InterruptedException, ExecutionException {
		testParallelRecord("http://localhost:" + getServerPort()
				+ "/content-demo/recorder-with-redirect", 200,
				"myfile-with-redirect", null, true);
	}

	private void testParallelRecord(String url, int statusCode,
			String contentType, String fileUploadedName, boolean interrupt)
			throws ClientProtocolException, IOException, InterruptedException,
			ExecutionException {
		ExecutorService execute = Executors.newFixedThreadPool(nThreads);
		Collection<Future<?>> futures = new LinkedList<Future<?>>();

		// Perform nThreads calls
		for (int i = 0; i < nThreads; i++) {
			futures.add(execute.submit(new ParallelTest(url, statusCode,
					fileUploadedName, contentType, interrupt)));
		}

		// Wait for all threads to be terminated
		for (Future<?> future : futures) {
			future.get();
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

	class ParallelTest implements Runnable {
		private String url;
		private int statusCode;
		private String contentType;
		private String fileUploadedName;
		private boolean interrupt;

		public ParallelTest(String url, int statusCode, String contentType,
				String fileUploadedName, boolean interrupt) {
			this.url = url;
			this.statusCode = statusCode;
			this.contentType = contentType;
			this.fileUploadedName = fileUploadedName;
			this.interrupt = interrupt;
		}

		public void run() {
			try {
				HttpClient client = new DefaultHttpClient();

				// Set automatic redirect for HTTP 307 (SC_TEMPORARY_REDIRECT)
				((DefaultHttpClient) client)
						.setRedirectStrategy(new DefaultRedirectStrategy() {
							public boolean isRedirected(HttpRequest request,
									HttpResponse response, HttpContext context)
									throws ProtocolException {
								boolean isRedirect = false;
								isRedirect = super.isRedirected(request,
										response, context);
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
				// HttpEndPointConfiguration config = new
				// HttpEndPointConfiguration();
				// fileUploaded = new File(config.getRecorderBaseDir()
				// + File.separator + fileUploadedName);

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
				final int responseStatusCode = response.getStatusLine()
						.getStatusCode();
				Assert.assertEquals("HTTP response status code must be "
						+ statusCode, statusCode, responseStatusCode);

				// Assert Content-Type in response
				if (contentType != null && resEntity.getContentType() != null) {
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
