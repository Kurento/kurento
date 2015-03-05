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
import org.kurento.test.client.Client;

/**
 * Browser configuration based for JSON test scenarios.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.1.0
 */
public class TestConfig {

	public static final String BROWSER = "browser";
	public static final String PRESENTER = "presenter";
	public static final String VIEWER = "viewer";

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

				if (instance.getInstances() > 0) {
					builder = builder.numInstances(instance.getInstances());
				}
				if (instance.getBrowserPerInstance() > 0) {
					builder = builder.browserPerInstance(instance
							.getBrowserPerInstance());
				}
				if (instance.getHostAddress() != null) {
					builder = builder.hostAddress(instance.getHostAddress());
				}
				if (instance.getLogin() != null) {
					builder = builder.login(instance.getLogin());
				}
				if (instance.getPasswd() != null) {
					builder = builder.passwd(instance.getPasswd());
				}
				if (instance.getKey() != null) {
					builder = builder.pem(instance.getKey());
				}
				if (instance.getPort() > 0) {
					builder = builder.serverPort(instance.getPort());
				}
				if (instance.isEnableScreenCapture()) {
					builder = builder.enableScreenCapture();
				}
				if (instance.getProtocol() != null) {
					builder = builder.protocol(Protocol.valueOf(instance
							.getProtocol().toUpperCase()));
				}
				if (instance.getClient() != null) {
					builder = builder.client(Client.value2Client(instance
							.getClient()));
				}
				if (instance.getPublicIP() != null) {
					builder = builder.publicIp(instance.getPublicIP());
				}
				if (instance.getSaucelabsUser() != null) {
					System.setProperty(BrowserClient.SAUCELAB_USER_PROPERTY,
							instance.getSaucelabsUser());
				}
				if (instance.getSaucelabsKey() != null) {
					System.setProperty(BrowserClient.SAUCELAB_KEY_PROPERTY,
							instance.getSaucelabsKey());
				}
				if (instance.isLocal()) {
					browserClient = builder.scope(BrowserScope.LOCAL).build();
				} else if (instance.isRemote()) {
					browserClient = builder.scope(BrowserScope.REMOTE).build();
				} else if (instance.isSauceLabs()) {
					if (instance.getVersion() == null
							|| instance.getPlatformType() == null) {
						throw new RuntimeException(
								"Platform and browser version should be configured in saucelabs tests");
					}
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
