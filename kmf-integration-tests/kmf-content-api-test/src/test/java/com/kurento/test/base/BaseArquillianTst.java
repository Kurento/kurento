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
package com.kurento.test.base;

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
import org.junit.experimental.categories.Category;

import com.kurento.kmf.commons.tests.ContentApiTests;

/**
 * Base class for Arquillian tests; it deploys the WAR in the Kurento
 * Application Server (KAS), i.e., a JBoss AS. This class also contains common
 * utilities for JUnit test classes (e.g. checksum calculation, JBoss server
 * port accessor, and so on).
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.0
 * @see <a href="http://arquillian.org/">Arquillian</a>
 */
@Category(ContentApiTests.class)
public class BaseArquillianTst {

	public static final Log log = LogFactory.getLog(BaseArquillianTst.class);

	public static final int BUFF = 1024;

	public static final String ALGORITHM = "MD5";

	@Deployment
	public static WebArchive createDeployment() throws IOException {
		InputStream inputStream = new FileInputStream(
				"target/test-classes/test.properties");
		Properties properties = new Properties();
		properties.load(inputStream);
		WebArchive war = ShrinkWrap
				.create(ZipImporter.class, "kmf-content-api-test.war")
				.importFrom(
						new File("target/"
								+ properties.getProperty("project.artifactId")
								+ "-"
								+ properties.getProperty("project.version")
								+ ".war")).as(WebArchive.class)
				.addPackages(true, "com.kurento");

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

	public String getServerPort() {
		return System.getProperty("JBOSS_SERVICE_PORT");
	}
}
