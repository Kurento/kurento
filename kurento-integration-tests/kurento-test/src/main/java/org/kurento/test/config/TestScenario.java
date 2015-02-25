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
import java.util.HashMap;
import java.util.Map;

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
	private final static String TEST_CONFIG_JSON_DEFAULT = "/test.conf.json";

	private static Logger log = LoggerFactory.getLogger(TestScenario.class);

	private Map<String, BrowserClient> browserMap;

	public TestScenario() {
		browserMap = new HashMap<>();
	}

	public void addBrowser(String id, BrowserClient browser) {
		assertKeyNotExist(id);
		browserMap.put(id, browser);
	}

	public void addBrowser(BrowserClient browser) {
		assertKeyNotExist(TestConfig.DEFAULT_BROWSER);
		browserMap.put(TestConfig.DEFAULT_BROWSER, browser);
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
		if (browserMap.isEmpty()) {
			out = "(browsers from node list)";
		}
		for (String key : browserMap.keySet()) {
			if (!out.isEmpty()) {
				out += ";";
			}
			out += "(id=" + key + ")";
			out += "(browserType=" + getBrowserType(key) + ")";
			out += "(scope=" + getScope(key) + ")";

			String browserVersion = getBrowserVersion(key);
			if (browserVersion != null) {
				out += browserVersion;
			}
			Platform platform = getPlatform(key);
			if (platform != null) {
				out += platform;
			}
		}
		return out;
	}

	/*
	 * No browsers (nor local neither remote).
	 */
	public static Collection<Object[]> noBrowsers() {
		// No browsers
		TestScenario test = new TestScenario();

		return Arrays.asList(new Object[][] { { test } });
	}

	/*
	 * Configuration based on JSON file
	 */
	public static Collection<Object[]> json() {
		try {
			String configJson = getProperty(TEST_CONFIG_JSON_PROPERTY);
			String jsonFile;
			if (configJson == null) {
				jsonFile = ConfigFileFinder.getPathInClasspath(
						TEST_CONFIG_JSON_DEFAULT).toString();
			} else {
				jsonFile = configJson;
			}

			// Read JSON and transform to GSON
			BufferedReader br = new BufferedReader(new FileReader(jsonFile));
			Gson gson = new Gson();
			TestConfig browserConfig = gson.fromJson(br, TestConfig.class);

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
		test1.addBrowser(new BrowserClient.Builder()
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
				.build());

		// Test #2 : Firefox in local
		TestScenario test2 = new TestScenario();
		test2.addBrowser(new BrowserClient.Builder()
				.browserType(BrowserType.FIREFOX).scope(BrowserScope.LOCAL)
				.build());

		return Arrays.asList(new Object[][] { { test1 }, { test2 } });
	}

	public static Collection<Object[]> localChrome(int port) {
		// Test #1 : Chrome in local
		TestScenario test = new TestScenario();
		test.addBrowser(new BrowserClient.Builder()
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
				.enableScreenCapture().protocol(Protocol.HTTPS).build());

		return Arrays.asList(new Object[][] { { test } });
	}

	public static Collection<Object[]> localChrome() {
		// Test #1 : Chrome in local
		TestScenario test = new TestScenario();
		test.addBrowser(new BrowserClient.Builder()
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
				.build());

		return Arrays.asList(new Object[][] { { test } });
	}

	public static Collection<Object[]> localPresenterAndViewer() {
		// Test #1 : Chrome in local (presenter and viewer)
		String videoPath = KurentoClientTest.getPathTestFiles()
				+ "/video/15sec/rgbHD.y4m";
		TestScenario test = new TestScenario();
		test.addBrowser(TestConfig.PRESENTER, new BrowserClient.Builder()
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
				.video(videoPath).build());
		test.addBrowser(TestConfig.VIEWER, new BrowserClient.Builder()
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
				.build());

		return Arrays.asList(new Object[][] { { test } });
	}

	public Map<String, BrowserClient> getBrowserMap() {
		return browserMap;
	}

	// TODO Refactor
	// public static Collection<Object[]> twoBrowsersInLocal() {
	// // Test #1 : Local Chrome as presenter and local Firefox as viewer
	// TestScenario test1 = new TestScenario();
	// test1.addBrowser(TestConfig.PRESENTER, new Browser.Builder(
	// BrowserType.CHROME).scope(BrowserScope.local).build());
	// test1.addBrowser(TestConfig.VIEWER, new Browser.Builder(
	// BrowserType.FIREFOX).scope(BrowserScope.local).build());
	//
	// // Test #2 : Local Firefox as presenter and local Chrome as viewer
	// TestScenario test2 = new TestScenario();
	// test2.addBrowser(TestConfig.PRESENTER, new Browser.Builder(
	// BrowserType.FIREFOX).scope(BrowserScope.local).build());
	// test2.addBrowser(TestConfig.VIEWER, new Browser.Builder(
	// BrowserType.CHROME).scope(BrowserScope.local).build());
	//
	// return Arrays.asList(new Object[][] { { test1 }, { test2 } });
	// }
	//
	// public static Collection<Object[]> twoBrowsersInLocalSingleTest() {
	// // Local Chrome as presenter and local Firefox as viewer
	// TestScenario test = new TestScenario();
	// test.addBrowser(TestConfig.PRESENTER, new Browser.Builder(
	// BrowserType.CHROME).scope(BrowserScope.local).build());
	// test.addBrowser(TestConfig.VIEWER, new Browser.Builder(
	// BrowserType.FIREFOX).scope(BrowserScope.local).build());
	//
	// return Arrays.asList(new Object[][] { { test } });
	// }
	//
	// /*
	// * Remote browsers (in saucelabs)
	// */
	// public static Collection<Object[]> oneBrowserInRemote() {
	// // Test #1 : Remote Chrome 38 in Windows 8
	// TestScenario test1 = new TestScenario();
	// test1.addBrowser(new Browser.Builder(BrowserType.CHROME)
	// .scope(BrowserScope.saucelabs).browserVersion("38")
	// .platform(Platform.WIN8).build());
	//
	// // Test #2 : Remote Chrome 39 in Windows 8
	// TestScenario test2 = new TestScenario();
	// test2.addBrowser(new Browser.Builder(BrowserType.CHROME)
	// .scope(BrowserScope.saucelabs).browserVersion("39")
	// .platform(Platform.WIN8).build());
	//
	// // Test #3 : Remote Chrome 40 in Windows 8
	// TestScenario test3 = new TestScenario();
	// test3.addBrowser(new Browser.Builder(BrowserType.CHROME)
	// .scope(BrowserScope.saucelabs).browserVersion("40")
	// .platform(Platform.WIN8).build());
	//
	// // Test #4 : Remote Chrome 38 in Linux
	// TestScenario test4 = new TestScenario();
	// test4.addBrowser(new Browser.Builder(BrowserType.CHROME)
	// .scope(BrowserScope.saucelabs).browserVersion("38")
	// .platform(Platform.LINUX).build());
	//
	// // Test #5 : Remote Chrome 39 in Linux
	// TestScenario test5 = new TestScenario();
	// test5.addBrowser(new Browser.Builder(BrowserType.CHROME)
	// .scope(BrowserScope.saucelabs).browserVersion("39")
	// .platform(Platform.LINUX).build());
	//
	// // Test #6 : Remote Chrome 40 in Linux
	// TestScenario test6 = new TestScenario();
	// test6.addBrowser(new Browser.Builder(BrowserType.CHROME)
	// .scope(BrowserScope.saucelabs).browserVersion("40")
	// .platform(Platform.LINUX).build());
	//
	// // Test #7 : Remote Firefox 34 in Windows 8
	// TestScenario test7 = new TestScenario();
	// test7.addBrowser(new Browser.Builder(BrowserType.FIREFOX)
	// .scope(BrowserScope.saucelabs).browserVersion("34")
	// .platform(Platform.WIN8).build());
	//
	// // Test #8 : Remote Firefox 35 in Windows 8
	// TestScenario test8 = new TestScenario();
	// test8.addBrowser(new Browser.Builder(BrowserType.FIREFOX)
	// .scope(BrowserScope.saucelabs).browserVersion("35")
	// .platform(Platform.WIN8).build());
	//
	// // Test #9 : Remote Firefox 34 in Linux
	// TestScenario test9 = new TestScenario();
	// test9.addBrowser(new Browser.Builder(BrowserType.FIREFOX)
	// .scope(BrowserScope.saucelabs).browserVersion("34")
	// .platform(Platform.LINUX).build());
	//
	// // Test #10 : Remote Firefox 35 in Linux
	// TestScenario test10 = new TestScenario();
	// test10.addBrowser(new Browser.Builder(BrowserType.FIREFOX)
	// .scope(BrowserScope.saucelabs).browserVersion("35")
	// .platform(Platform.LINUX).build());
	//
	// return Arrays.asList(new Object[][] { { test1 }, { test2 }, { test3 },
	// { test4 }, { test5 }, { test6 }, { test7 }, { test8 },
	// { test9 }, { test10 } });
	// }
	//
	// public static Collection<Object[]> twoBrowsersInRemote() {
	// Browser genericPresenter = new Browser.Builder(BrowserType.CHROME)
	// .scope(BrowserScope.saucelabs).browserVersion("40")
	// .platform(Platform.LINUX).build();
	//
	// // Test #1 : Remote Chrome 38 in Windows 8
	// TestScenario test1 = new TestScenario();
	// test1.addBrowser(TestConfig.PRESENTER, genericPresenter);
	// test1.addBrowser(TestConfig.VIEWER, new Browser.Builder(
	// BrowserType.CHROME).scope(BrowserScope.saucelabs)
	// .browserVersion("38").platform(Platform.WIN8).build());
	//
	// // Test #2 : Remote Chrome 39 in Windows 8
	// TestScenario test2 = new TestScenario();
	// test2.addBrowser(TestConfig.PRESENTER, genericPresenter);
	// test2.addBrowser(TestConfig.VIEWER, new Browser.Builder(
	// BrowserType.CHROME).scope(BrowserScope.saucelabs)
	// .browserVersion("39").platform(Platform.WIN8).build());
	//
	// // Test #3 : Remote Chrome 40 in Windows 8
	// TestScenario test3 = new TestScenario();
	// test3.addBrowser(TestConfig.PRESENTER, genericPresenter);
	// test3.addBrowser(TestConfig.VIEWER, new Browser.Builder(
	// BrowserType.CHROME).scope(BrowserScope.saucelabs)
	// .browserVersion("40").platform(Platform.WIN8).build());
	//
	// // Test #4 : Remote Chrome 38 in Linux
	// TestScenario test4 = new TestScenario();
	// test4.addBrowser(TestConfig.PRESENTER, genericPresenter);
	// test4.addBrowser(TestConfig.VIEWER, new Browser.Builder(
	// BrowserType.CHROME).scope(BrowserScope.saucelabs)
	// .browserVersion("38").platform(Platform.LINUX).build());
	//
	// // Test #5 : Remote Chrome 39 in Linux
	// TestScenario test5 = new TestScenario();
	// test5.addBrowser(TestConfig.PRESENTER, genericPresenter);
	// test5.addBrowser(TestConfig.VIEWER, new Browser.Builder(
	// BrowserType.CHROME).scope(BrowserScope.saucelabs)
	// .browserVersion("39").platform(Platform.LINUX).build());
	//
	// // Test #6 : Remote Chrome 40 in Linux
	// TestScenario test6 = new TestScenario();
	// test6.addBrowser(TestConfig.PRESENTER, genericPresenter);
	// test6.addBrowser(TestConfig.VIEWER, new Browser.Builder(
	// BrowserType.CHROME).scope(BrowserScope.saucelabs)
	// .browserVersion("40").platform(Platform.LINUX).build());
	//
	// // Test #7 : Remote Firefox 34 in Windows 8
	// TestScenario test7 = new TestScenario();
	// test7.addBrowser(TestConfig.PRESENTER, genericPresenter);
	// test7.addBrowser(TestConfig.VIEWER, new Browser.Builder(
	// BrowserType.FIREFOX).scope(BrowserScope.saucelabs)
	// .browserVersion("34").platform(Platform.WIN8).build());
	//
	// // Test #8 : Remote Firefox 35 in Windows 8
	// TestScenario test8 = new TestScenario();
	// test8.addBrowser(TestConfig.PRESENTER, genericPresenter);
	// test8.addBrowser(TestConfig.VIEWER, new Browser.Builder(
	// BrowserType.FIREFOX).scope(BrowserScope.saucelabs)
	// .browserVersion("35").platform(Platform.WIN8).build());
	//
	// // Test #9 : Remote Firefox 34 in Linux
	// TestScenario test9 = new TestScenario();
	// test9.addBrowser(TestConfig.PRESENTER, genericPresenter);
	// test9.addBrowser(TestConfig.VIEWER, new Browser.Builder(
	// BrowserType.FIREFOX).scope(BrowserScope.saucelabs)
	// .browserVersion("34").platform(Platform.LINUX).build());
	//
	// // Test #10 : Remote Firefox 35 in Linux
	// TestScenario test10 = new TestScenario();
	// test10.addBrowser(TestConfig.PRESENTER, genericPresenter);
	// test10.addBrowser(TestConfig.VIEWER, new Browser.Builder(
	// BrowserType.FIREFOX).scope(BrowserScope.saucelabs)
	// .browserVersion("35").platform(Platform.LINUX).build());
	//
	// // Test #11 : Remote IExplorer 8 in Windows 7
	// TestScenario test11 = new TestScenario();
	// test11.addBrowser(TestConfig.PRESENTER, genericPresenter);
	// test11.addBrowser(TestConfig.VIEWER, new Browser.Builder(
	// BrowserType.IEXPLORER).scope(BrowserScope.saucelabs)
	// .browserVersion("8").platform(Platform.VISTA).build());
	//
	// // Test #12 : Remote IExplorer 9 in Windows 7
	// TestScenario test12 = new TestScenario();
	// test12.addBrowser(TestConfig.PRESENTER, genericPresenter);
	// test12.addBrowser(TestConfig.VIEWER, new Browser.Builder(
	// BrowserType.IEXPLORER).scope(BrowserScope.saucelabs)
	// .browserVersion("9").platform(Platform.VISTA).build());
	//
	// // Test #13 : Remote IExplorer 10 in Windows 7
	// TestScenario test13 = new TestScenario();
	// test13.addBrowser(TestConfig.PRESENTER, genericPresenter);
	// test13.addBrowser(TestConfig.VIEWER, new Browser.Builder(
	// BrowserType.IEXPLORER).scope(BrowserScope.saucelabs)
	// .browserVersion("10").platform(Platform.VISTA).build());
	//
	// // Test #14 : Remote IExplorer 11 in Windows 7
	// TestScenario test14 = new TestScenario();
	// test14.addBrowser(TestConfig.PRESENTER, genericPresenter);
	// test14.addBrowser(TestConfig.VIEWER, new Browser.Builder(
	// BrowserType.IEXPLORER).scope(BrowserScope.saucelabs)
	// .browserVersion("11").platform(Platform.VISTA).build());
	//
	// return Arrays.asList(new Object[][] { { test1 }, { test2 }, { test3 },
	// { test4 }, { test5 }, { test6 }, { test7 }, { test8 },
	// { test9 }, { test10 }, { test11 }, { test12 }, { test13 },
	// { test14 } });
	// }
	//
	// public static Collection<Object[]> twoBrowsersInRemoteSingleTest() {
	// // Test #1 : Remote Chrome and Firefox
	// TestScenario test = new TestScenario();
	// test.addBrowser(TestConfig.PRESENTER, new Browser.Builder(
	// BrowserType.CHROME).scope(BrowserScope.saucelabs)
	// .browserVersion("40").platform(Platform.LINUX).build());
	// test.addBrowser(TestConfig.VIEWER, new Browser.Builder(
	// BrowserType.FIREFOX).scope(BrowserScope.saucelabs)
	// .browserVersion("35").platform(Platform.LINUX).build());
	// return Arrays.asList(new Object[][] { { test } });
	// }
	// /*
	// * Mixed browsers (local and in remote, i.e. from saucelabs)
	// */
	// public static Collection<Object[]> localPresenterRemoteViewers(int
	// nViewers) {
	// // Test #1 : Remote Chrome and Firefox
	// TestScenario test = new TestScenario();
	// test.addBrowser(TestConfig.PRESENTER, new Browser.Builder(
	// BrowserType.CHROME).scope(BrowserScope.local).build());
	//
	// for (int i = 1; i <= nViewers; i++) {
	// test.addBrowser(TestConfig.VIEWER + i, new Browser.Builder(
	// BrowserType.CHROME).scope(BrowserScope.saucelabs)
	// .browserVersion("40").platform(Platform.LINUX).build());
	// }
	// return Arrays.asList(new Object[][] { { test } });
	// }

}
