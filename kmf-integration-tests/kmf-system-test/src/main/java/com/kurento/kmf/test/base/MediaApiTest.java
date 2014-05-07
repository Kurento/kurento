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

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.LifecycleException;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.test.HttpServer;
import com.kurento.kmf.test.PortManager;

import freemarker.template.TemplateException;

/**
 * Base for tests using kmf-media-api and Spring Boot.
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 * @see <a href="http://projects.spring.io/spring-boot/">Spring Boot</a>
 */
public class MediaApiTest extends BaseTest {

	public static Logger log = LoggerFactory.getLogger(MediaApiTest.class);

	protected MediaPipelineFactory pipelineFactory;
	private HttpServer server;

	@Before
	public void setup() throws Exception {

		setupKurentoServer();
		setupMediaPipelineFactory();
		setupHttpServer();

	}

	@After
	public void teardown() throws LifecycleException {

		teardownPipelineFactory();
		teardownHttpServer();
		teardownKurentoServer();
	}

	protected void setupMediaPipelineFactory() throws IOException,
			TemplateException, InterruptedException {

		pipelineFactory = new MediaPipelineFactory(serverAddress, serverPort,
				handlerAddress, handlerPort);
	}

	protected void setupHttpServer() throws ServletException, IOException,
			LifecycleException {

		server = new HttpServer(PortManager.getPort());
		server.start();
	}

	protected void teardownHttpServer() throws LifecycleException {
		if (server != null) {
			server.destroy();
		}
	}

	protected void teardownPipelineFactory() {
		if (pipelineFactory != null) {
			pipelineFactory.destroy();
		}
	}

}
