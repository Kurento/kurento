package com.kurento.demo.junit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class BaseArquillianTst {

	public static final Log log = LogFactory.getLog(BaseArquillianTst.class);

	public static final int BUFF = 1024;

	public static final String ALGORITHM = "MD5";

	@Deployment
	public static WebArchive createDeployment() {
		WebArchive war = ShrinkWrap
				.create(ZipImporter.class, "content-api-test.war")
				.importFrom(
						new File("target/content-api-test-1.0.0-SNAPSHOT.war"))
				.as(WebArchive.class).addPackages(true, "com.kurento");
		log.info(war.toString(true));
		return war;
	}

	public String createChecksum(File file) throws IOException,
			NoSuchAlgorithmException {
		return createChecksum(new FileInputStream(file));
	}

	public String createChecksum(InputStream inputStream) throws IOException,
			NoSuchAlgorithmException {
		byte[] buffer = new byte[BUFF];
		MessageDigest complete = MessageDigest.getInstance(ALGORITHM);
		int numRead;
		do {
			numRead = inputStream.read(buffer);
			if (numRead > 0) {
				complete.update(buffer, 0, numRead);
			}
		} while (numRead != -1);
		inputStream.close();
		return new String(complete.digest());
	}

	public String getServerPort() throws IOException {
		InputStream inputStream = new FileInputStream(
				"target/test-classes/test.properties");
		Properties properties = new Properties();
		properties.load(inputStream);
		System.out.println();
		return properties.getProperty("jboss-as.service.port");
	}
}
