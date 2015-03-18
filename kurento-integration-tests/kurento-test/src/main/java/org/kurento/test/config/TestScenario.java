/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
package org.kurento.test.config;

import static org.kurento.commons.PropertiesManager.getProperty;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Assert;
import org.kurento.commons.ConfigFileFinder;
import org.kurento.test.base.KurentoClientTest;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.BrowserType;
import org.openqa.selenium.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Scenarios for test (e.g. one local browser and other in remote...)
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.1.0
 */
public class TestScenario {

	private final static String TEST_CONFIG_JSON_PROPERTY = "test.config.json";
	private final static String TEST_CONFIG_JSON_DEFAULT = "test.conf.json";

	private static Logger log = LoggerFactory.getLogger(TestScenario.class);

	private Map<String, BrowserClient> browserMap;

	public TestScenario() {
		browserMap = new TreeMap<>();
	}

	public void addBrowser(String id, BrowserClient browser) {
		if (browser.getNumInstances() > 0) {
			for (int i = 0; i < browser.getNumInstances(); i++) {
				if (browser.getBrowserPerInstance() > 1) {
					for (int j = 0; j < browser.getBrowserPerInstance(); j++) {
						String browserId = (browser.getNumInstances() == 1) ? id
								+ j
								: id + i + "-" + j;
						addBrowserInstance(browserId,
								new BrowserClient(browser.getBuilder()));
					}
				} else {
					addBrowserInstance(id + i,
							new BrowserClient(browser.getBuilder()));
				}
			}
		} else {
			addBrowserInstance(id, browser);
		}
	}

	private void addBrowserInstance(String id, BrowserClient browser) {
		assertKeyNotExist(id);
		browser.setId(id);
		browserMap.put(id, browser);
	}

	private void assertKeyNotExist(String key) {
		Assert.assertFalse("'" + key
				+ "' key already registered in browser config map", browserMap
				.keySet().contains(key));
	}

	public BrowserScope getScope(String key) {
		return (browserMap.get(key)).getScope();
	}

	public BrowserType getBrowserType(String key) {
		return (browserMap.get(key)).getBrowserType();
	}

	public Platform getPlatform(String key) {
		return (browserMap.get(key)).getPlatform();
	}

	public String getBrowserVersion(String key) {
		return (browserMap.get(key)).getBrowserVersion();
	}

	@Override
	public String toString() {
		String out = "";
		for (String key : browserMap.keySet()) {
			if (!out.isEmpty()) {
				out += ", ";
			}
			else {
				out += "Number of browser(s) = " + browserMap.size() + " : ";
			}
			out += "(id=" + key + ")";
			out += "(browserType=" + getBrowserType(key) + ")";
			out += "(scope=" + getScope(key) + ")";

			String browserVersion = getBrowserVersion(key);
			if (browserVersion != null) {
				out += "(browserVersion=" + browserVersion + ")";
			}
			Platform platform = getPlatform(key);
			if (platform != null) {
				out += "(platform=" + platform + ")";
			}
		}
		return out;
	}

	/*
	 * Configuration based on JSON file
	 */
	public static Collection<Object[]> json() {
		return json(TEST_CONFIG_JSON_DEFAULT);
	}

	public static Collection<Object[]> json(String jsonFile) {
		try {
			String configJson = getProperty(TEST_CONFIG_JSON_PROPERTY);
			String jsonPath;
			if (configJson == null) {
				jsonPath = ConfigFileFinder.getPathInClasspath("/" + jsonFile)
						.toString();
			} else {
				jsonPath = configJson;
			}

			// Read JSON and transform to GSON
			BufferedReader br = new BufferedReader(new FileReader(jsonPath));
			Gson gson = new Gson();
			BrowserConfig browserConfig = gson.fromJson(br, BrowserConfig.class);

			return browserConfig.getTestScenario();

		} catch (IOException e) {
			log.error(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	/*
	 * Local browsers
	 */
	public static Collection<Object[]> localChromeAndFirefox() {
		// Test #1 : Chrome in local
		TestScenario test1 = new TestScenario();
		test1.addBrowser(BrowserConfig.BROWSER, new BrowserClient.Builder()
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
				.build());
		// Test #2 : Firefox in local
		TestScenario test2 = new TestScenario();
		test2.addBrowser(BrowserConfig.BROWSER, new BrowserClient.Builder()
				.browserType(BrowserType.FIREFOX).scope(BrowserScope.LOCAL)
				.build());

		return Arrays.asList(new Object[][] { { test1 }, { test2 } });
	}

	public static Collection<Object[]> localChrome() {
		// Test: Chrome in local
		TestScenario test = new TestScenario();
		test.addBrowser(BrowserConfig.BROWSER, new BrowserClient.Builder()
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
				.build());

		return Arrays.asList(new Object[][] { { test } });
	}

	public static Collection<Object[]> localFirefox() {
		// Test: Firefox in local
		TestScenario test = new TestScenario();
		test.addBrowser(BrowserConfig.BROWSER, new BrowserClient.Builder()
				.browserType(BrowserType.FIREFOX).scope(BrowserScope.LOCAL)
				.build());

		return Arrays.asList(new Object[][] { { test } });
	}

	public static Collection<Object[]> localPresenterAndViewer() {
		// Test: Chrome in local (presenter and viewer)
		TestScenario test = new TestScenario();
		test.addBrowser(BrowserConfig.PRESENTER, new BrowserClient.Builder()
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
				.build());
		test.addBrowser(BrowserConfig.VIEWER, new BrowserClient.Builder()
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
				.build());

		return Arrays.asList(new Object[][] { { test } });
	}

	public static Collection<Object[]> localPresenterAndViewerRGB() {
		// Test: Chrome in local (presenter and viewer)
		String videoPath = KurentoClientTest.getPathTestFiles()
				+ "/video/15sec/rgbHD.y4m";
		TestScenario test = new TestScenario();
		test.addBrowser(BrowserConfig.PRESENTER, new BrowserClient.Builder()
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
				.video(videoPath).build());
		test.addBrowser(BrowserConfig.VIEWER, new BrowserClient.Builder()
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
				.video(videoPath).build());

		return Arrays.asList(new Object[][] { { test } });
	}

	public Map<String, BrowserClient> getBrowserMap() {
		return browserMap;
	}
}
