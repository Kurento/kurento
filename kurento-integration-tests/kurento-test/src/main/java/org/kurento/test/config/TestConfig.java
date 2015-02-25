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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.kurento.test.client.BrowserClient;

/**
 * Browser configuration based for JSON test scenarios.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.1.0
 */
public class TestConfig {

	public static final String PRESENTER = "presenter";
	public static final String VIEWER = "viewer";
	public static final String DEFAULT_BROWSER = "browser";

	private List<Map<String, BrowserInstance>> executions;

	public TestConfig() {
		executions = new ArrayList<>();
	}

	public List<Map<String, BrowserInstance>> getExecutions() {
		return executions;
	}

	public void setExecutions(List<Map<String, BrowserInstance>> executions) {
		this.executions = executions;
	}

	@Override
	public String toString() {
		return "TestConfig [executions=" + executions + "]";
	}

	public Collection<Object[]> getTestScenario() {
		Collection<Object[]> tests = new ArrayList<>();
		for (Map<String, BrowserInstance> browser : executions) {

			TestScenario test = new TestScenario();
			for (String key : browser.keySet()) {
				BrowserClient browserClient = null;
				BrowserInstance instance = browser.get(key);

				BrowserClient.Builder builder = new BrowserClient.Builder()
						.browserType(instance.getBrowserType());

				if (instance.isLocal()) {
					browserClient = builder.scope(BrowserScope.LOCAL).build();
				} else if (instance.isRemote()) {
					browserClient = builder.scope(BrowserScope.REMOTE).build();
				} else if (instance.isSauceLabs()) {
					browserClient = builder.scope(BrowserScope.SAUCELABS)
							.browserVersion(instance.getVersion())
							.platform(instance.getPlatformType()).build();
				} else {
					throw new RuntimeException(
							"Unknown scope in JSON configuration: "
									+ instance.getScope());
				}
				test.addBrowser(key, browserClient);
			}
			tests.add(new Object[] { test });
		}
		return tests;
	}
}
