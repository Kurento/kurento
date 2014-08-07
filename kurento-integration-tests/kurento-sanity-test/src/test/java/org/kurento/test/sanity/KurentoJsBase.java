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
 */
package org.kurento.test.sanity;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.kurento.client.factory.KurentoProperties;
import org.kurento.test.base.GridBrowserKurentoClientTest;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Base for kurento-js sanity tests.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
public class KurentoJsBase {

	protected static final Logger log = LoggerFactory
			.getLogger(KurentoJsBase.class);

	protected WebDriver driver;
	protected String serverAddress;
	protected int serverPort;
	protected String kurentoUrl;
	protected String[] kurentoLibs = { "kurento-client", "kurento-client.min",
			"kurento-utils", "kurento-utils.min" };

	protected static final String DEFAULT_KURENTO_JS_URL = "http://builds.kurento.org/release/stable/";

	@Before
	public void setup() {
		driver = new FirefoxDriver();
		serverAddress = KurentoProperties.getThriftKcsAddress().getHost();
		serverPort = KurentoServicesTestHelper.getAppHttpPort();

		createHtmlPages();
		KurentoServicesTestHelper.startHttpServer();
	}

	private void createHtmlPages() {
		try {
			final String outputFolder = new ClassPathResource("static")
					.getFile().getAbsolutePath() + File.separator;

			Configuration cfg = new Configuration();
			cfg.setClassForTemplateLoading(GridBrowserKurentoClientTest.class,
					"/templates/");
			Template template = cfg.getTemplate("kurento-client.html.ftl");

			Map<String, Object> data = new HashMap<String, Object>();
			data.put("kurentoUrl", kurentoUrl);

			for (String lib : kurentoLibs) {
				Writer writer = new FileWriter(new File(outputFolder + lib
						+ ".html"));
				data.put("kurentoLib", lib);

				if (lib.contains("utils")) {
					data.put("kurentoObject", "kurentoUtils");
				} else {
					data.put("kurentoObject", "KurentoClient");
				}

				template.process(data, writer);
				writer.flush();
				writer.close();
			}
		} catch (Exception e) {
			Assert.fail("Exception creating templates: " + e.getMessage());
		}

	}

	public void doTest() {
		for (String lib : kurentoLibs) {
			final String urlTest = "http://" + serverAddress + ":" + serverPort
					+ "/" + lib + ".html";
			driver.get(urlTest);
			log.debug("Launching kurento-js sanity test against {}", urlTest);

			String status = driver.findElement(By.id("status")).getAttribute(
					"value");

			Assert.assertTrue("Sanity test for " + lib + " failed (" + status
					+ ")", status.equals("Ok"));
		}
	}

	@After
	public void end() {
		driver.close();
		KurentoServicesTestHelper.teardownHttpServer();
	}

}
