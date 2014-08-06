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
package org.kurento.kmf.test.sanity;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import org.kurento.kmf.media.factory.KmfMediaApiProperties;
import org.kurento.kmf.test.base.GridBrowserMediaApiTest;
import org.kurento.kmf.test.services.KurentoServicesTestHelper;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Base for KWS sanity tests.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
public class KwsBase {

	protected static final Logger log = LoggerFactory.getLogger(KwsBase.class);

	protected WebDriver driver;
	protected String serverAddress;
	protected int serverPort;
	protected String kwsUrl;
	protected String[] kwsLibs = { "kws-media-api", "kws-media-api.min",
			"kws-utils", "kws-utils.min" };

	protected static final String DEFAULT_KWS_URL = "http://builds.kurento.org/release/stable/";

	@Before
	public void setup() {
		driver = new FirefoxDriver();
		serverAddress = KmfMediaApiProperties.getThriftKmfAddress().getHost();
		serverPort = KurentoServicesTestHelper.getAppHttpPort();

		createHtmlPages();
		KurentoServicesTestHelper.startHttpServer();
	}

	private void createHtmlPages() {
		try {
			final String outputFolder = new ClassPathResource("static")
					.getFile().getAbsolutePath() + File.separator;

			Configuration cfg = new Configuration();
			cfg.setClassForTemplateLoading(GridBrowserMediaApiTest.class,
					"/templates/");
			Template template = cfg.getTemplate("kws-media-api.html.ftl");

			Map<String, Object> data = new HashMap<String, Object>();
			data.put("kwsUrl", kwsUrl);

			for (String lib : kwsLibs) {
				Writer writer = new FileWriter(new File(outputFolder + lib
						+ ".html"));
				data.put("kwsLib", lib);

				if (lib.contains("utils")) {
					data.put("kwsObject", "kwsUtils");
				} else {
					data.put("kwsObject", "KwsMedia");
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
		for (String lib : kwsLibs) {
			final String urlTest = "http://" + serverAddress + ":" + serverPort
					+ "/" + lib + ".html";
			driver.get(urlTest);
			log.debug("Launching KWS sanity test against {}", urlTest);

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
