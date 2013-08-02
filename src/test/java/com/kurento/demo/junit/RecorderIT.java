package com.kurento.demo.junit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.kurento.kmf.media.HttpEndPointConfiguration;

@RunWith(Arquillian.class)
public class RecorderIT extends BaseArquillianTst {

	private static final int BUFF = 1024;

	private File fileFromUrl;

	private File fileUploaded;

	@Before
	public void setUp() throws IOException {
		// Sample file from the web
		URL url = new URL(
				"http://hc.apache.org/httpcomponents-client-ga/tutorial/pdf/httpclient-tutorial.pdf");
		InputStream input = url.openStream();

		// Temporal file
		fileFromUrl = new File("fileFromUrl");
		deleteFile(fileFromUrl);

		OutputStream output = new FileOutputStream(fileFromUrl);
		byte[] buf = new byte[BUFF];
		int len;
		while ((len = input.read(buf)) > 0) {
			output.write(buf, 0, len);
		}
		output.close();
		input.close();
	}

	@Test
	public void testRecord() throws ClientProtocolException, IOException {
		HttpClient client = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(
				"http://localhost:8180/content-demo/upload");
		MultipartEntity entity = new MultipartEntity(
				HttpMultipartMode.BROWSER_COMPATIBLE);

		FileBody fileBody = new FileBody(fileFromUrl);
		final String output = "myfile";
		entity.addPart(output, fileBody);

		httpPost.setEntity(entity);
		// HttpResponse response =
		client.execute(httpPost);

		HttpEndPointConfiguration config = new HttpEndPointConfiguration();
		fileUploaded = new File(config.getRecorderBaseDir() + File.separator
				+ output);
		Assert.assertTrue("File has not been uploaded correctly",
				fileUploaded.exists());

	}

	@After
	public void close() {
		// Delete temporal file, uploaded file, and folder
		deleteFile(fileFromUrl);
		deleteFile(fileUploaded);
		HttpEndPointConfiguration config = new HttpEndPointConfiguration();
		deleteFile(new File(config.getRecorderBaseDir()));
	}

	private void deleteFile(File file) {
		if (file.exists()) {
			log.info("file delete " + file.delete());
		}
	}
}
