/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
 */package com.kurento.kmf.test.base;

import org.apache.catalina.LifecycleException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.test.HttpServer;
import com.kurento.kmf.test.KurentoMediaServer;
import com.kurento.kmf.test.PortManager;
import com.kurento.kmf.test.PropertiesManager;

/**
 * Base for tests using kmf-media-api and Spring Boot.
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 * @see <a href="http://projects.spring.io/spring-boot/">Spring Boot</a>
 */
public class MediaApiTest {

	protected MediaPipelineFactory pipelineFactory;
	private HttpServer server;
	public static Logger log = LoggerFactory.getLogger(MediaApiTest.class);
	private KurentoMediaServer kms;

	@Rule
	public TestName testName = new TestName();

	@Before
	public void setup() throws Exception {
		String serverAddress = PropertiesManager.getSystemProperty(
				"kurento.serverAddress", "127.0.0.1");
		int serverPort = PropertiesManager.getSystemProperty(
				"kurento.serverPort", 9090);

		String handlerAddress = PropertiesManager.getSystemProperty(
				"kurento.handlerAddress", "127.0.0.1");
		int handlerPort = PropertiesManager.getSystemProperty(
				"kurento.handlerPort", 9104);

		pipelineFactory = new MediaPipelineFactory(serverAddress, serverPort,
				handlerAddress, handlerPort);

		server = new HttpServer(PortManager.getPort());
		server.start();

		int httpEndpointPort = PropertiesManager.getSystemProperty(
				"httpEPServer.serverPort", 9091);

		// KMS
		kms = new KurentoMediaServer(serverAddress, serverPort,
				httpEndpointPort);
		if (kms.isConfigAvailable()) {
			kms.start(testName.getMethodName());
		}
	}

	@After
	public void teardown() throws LifecycleException {
		pipelineFactory.destroy();
		server.destroy();

		if (kms.isConfigAvailable()) {
			kms.stop();
		}
	}

	public int getServerPort() {
		return PortManager.getPort();
	}

}
