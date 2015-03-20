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
package org.kurento.test.base;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.TestClient;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.TestScenario;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base for Kurento tests.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */

@RunWith(Parameterized.class)
public class KurentoTest {

	public static Logger log = LoggerFactory.getLogger(KurentoTest.class);
	public static final Color CHROME_VIDEOTEST_COLOR = new Color(0, 135, 0);

	@Rule
	public TestName testName = new TestName();

	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] { {} });
	}

	private TestClient client;
	private TestScenario testScenario;

	public KurentoTest() {
	}

	public KurentoTest(TestScenario testScenario) {
		client = new TestClient();
		this.testScenario = testScenario;
	}

	@Before
	public void setupKurentoTest() {
		if (testScenario != null) {
			for (String browserKey : testScenario.getBrowserMap().keySet()) {
				BrowserClient browserClient = testScenario.getBrowserMap().get(
						browserKey);
				initBrowserClient(browserKey, browserClient);
			}
		}
	}

	private void initBrowserClient(String browserKey,
			BrowserClient browserClient) {
		browserClient.setId(browserKey);
		browserClient.setName(testName.getMethodName());
		browserClient.init();
		browserClient.injectKurentoTestJs();
	}

	@After
	public void teardownKurentoTest() {
		if (testScenario != null) {
			for (BrowserClient browserClient : testScenario.getBrowserMap()
					.values()) {
				try {
					browserClient.close();
				} catch (UnreachableBrowserException e) {
					log.warn(e.getMessage());
				}
			}
		}
	}

	public TestScenario getTestScenario() {
		return testScenario;
	}

	public int getTimeout() {
		return client.getBrowserClient().getTimeout();
	}

	public void addBrowserClient(String browserKey, BrowserClient browserClient) {
		testScenario.getBrowserMap().put(browserKey, browserClient);
		initBrowserClient(browserKey, browserClient);
	}

	public void setClient(TestClient client) {
		this.client = client;
	}

	public TestClient getBrowser(String browserKey) {
		return assertAndGetBrowser(browserKey);
	}

	public TestClient getBrowser() {
		try {
			return assertAndGetBrowser(BrowserConfig.BROWSER);

		} catch (RuntimeException e) {
			if (testScenario.getBrowserMap().isEmpty()) {
				throw new RuntimeException(
						"Empty test scenario: no available browser to run tests!");
			} else {
				String browserKey = testScenario.getBrowserMap().entrySet()
						.iterator().next().getKey();
				log.debug(BrowserConfig.BROWSER
						+ " is not registered in test scenarario, instead"
						+ " using first browser in the test scenario, i.e. "
						+ browserKey);

				client.setBrowserClient(testScenario.getBrowserMap().get(
						browserKey));
				return client.clone();
			}
		}
	}

	public TestClient getBrowser(int index) {
		return assertAndGetBrowser(BrowserConfig.BROWSER + index);
	}

	public TestClient getPresenter() {
		return assertAndGetBrowser(BrowserConfig.PRESENTER);
	}

	public TestClient getPresenter(int index) {
		return assertAndGetBrowser(BrowserConfig.PRESENTER + index);
	}

	public TestClient getViewer() {
		return assertAndGetBrowser(BrowserConfig.VIEWER);
	}

	public TestClient getViewer(int index) {
		return assertAndGetBrowser(BrowserConfig.VIEWER + index);
	}

	private TestClient assertAndGetBrowser(String browserKey) {
		if (!testScenario.getBrowserMap().keySet().contains(browserKey)) {
			throw new RuntimeException(browserKey
					+ " is not registered as browser in the test scenario");
		}

		client.setBrowserClient(testScenario.getBrowserMap().get(browserKey));
		return client.clone();
	}

}
