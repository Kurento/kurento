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
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.kurento.commons.testing.SanityTests;
import org.kurento.test.base.BrowserKurentoClientTest;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.config.TestScenario;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import freemarker.template.Configuration;
import freemarker.template.Template;
import io.github.bonigarcia.wdm.ChromeDriverManager;

/**
 * Base for kurento-js sanity tests.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
@Category(SanityTests.class)
public class KurentoJsBase extends BrowserKurentoClientTest {

	protected static final Logger log = LoggerFactory.getLogger(KurentoJsBase.class);

	protected WebDriver driver;
	protected String serverAddress;
	protected int serverPort;
	protected String kurentoUrl;
	protected String[] kurentoLibs = { "kurento-client", "kurento-client.min", "kurento-utils", "kurento-utils.min" };

	protected static final String DEFAULT_KURENTO_JS_URL = "http://builds.kurento.org/release/stable/";

	public KurentoJsBase(TestScenario testScenario) {
		super(testScenario);
	}

	@BeforeClass
	public static void setupClass() {
		ChromeDriverManager.getInstance().setup();
	}

	@Before
	public void setup() {
		driver = BrowserClient.newWebDriver(new ChromeOptions());

		serverAddress = "127.0.0.1";
		serverPort = KurentoServicesTestHelper.getAppHttpPort();
		log.debug("serverPort = {}", serverPort);

		createHtmlPages();
	}

	private void createHtmlPages() {
		try {
			final String outputFolder = new ClassPathResource("static").getFile().getAbsolutePath() + File.separator;

			Configuration cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
			cfg.setClassForTemplateLoading(KurentoJsBase.class, "/templates/");
			Template template = cfg.getTemplate("kurento-client.html.ftl");

			Map<String, Object> data = new HashMap<String, Object>();
			data.put("kurentoUrl", kurentoUrl);

			for (String lib : kurentoLibs) {
				Writer writer = new FileWriter(new File(outputFolder + lib + ".html"));
				data.put("kurentoLib", lib);

				if (lib.contains("utils")) {
					data.put("kurentoObject", "kurentoUtils");
				} else {
					data.put("kurentoObject", "kurentoClient");
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
			final String urlTest = "http://" + serverAddress + ":" + serverPort + "/" + lib + ".html";
			driver.get(urlTest);

			log.debug("Launching kurento-js sanity test against {}", urlTest);

			String status = driver.findElement(By.id("status")).getAttribute("value");

			Assert.assertTrue("Sanity test for " + lib + " failed (" + status + ")", status.equals("Ok"));
		}
	}

	@After
	public void end() {
		if (driver != null) {
			driver.close();
		}
	}

}
