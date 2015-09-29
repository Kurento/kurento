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

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.TestConfiguration.TEST_URL_TIMEOUT_DEFAULT;
import static org.kurento.test.TestConfiguration.TEST_URL_TIMEOUT_PROPERTY;

import java.awt.Color;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.After;
import org.junit.Assert;
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
import org.kurento.test.internal.AbortableCountDownLatch;
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
public abstract class KurentoTest<E extends TestClient> {

	public static Logger log = LoggerFactory.getLogger(KurentoTest.class);
	public static final Color CHROME_VIDEOTEST_COLOR = new Color(0, 135, 0);

	@Rule
	public TestName testName = new TestName();

	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] { {} });
	}

	private Map<String, E> clients = new ConcurrentHashMap<>();
	private TestScenario testScenario;

	public KurentoTest() {
	}

	public KurentoTest(TestScenario testScenario) {
		this.testScenario = testScenario;
	}

	@Before
	public void setupKurentoTest() throws InterruptedException {
		if (testScenario != null && testScenario.getBrowserMap() != null && testScenario.getBrowserMap().size() > 0) {
			ExecutorService executor = Executors.newFixedThreadPool(testScenario.getBrowserMap().size());
			final AbortableCountDownLatch latch = new AbortableCountDownLatch(testScenario.getBrowserMap().size());
			for (final String browserKey : testScenario.getBrowserMap().keySet()) {

				executor.execute(new Runnable() {

					@Override
					public void run() {
						try {
							BrowserClient browserClient = testScenario.getBrowserMap().get(browserKey);

							int timeout = getProperty(TEST_URL_TIMEOUT_PROPERTY, TEST_URL_TIMEOUT_DEFAULT);

							URL url = browserClient.getUrl();
							if (!testScenario.getUrlList().contains(url)) {
								waitForHostIsReachable(url, timeout);
								testScenario.getUrlList().add(url);
							}
							initBrowserClient(browserKey, browserClient);
							latch.countDown();
						} catch (Throwable t) {
							latch.abort("Exception setting up test. A browser could not be initialised", t);
							t.printStackTrace();
						}
					}
				});
			}

			latch.await();
		}
	}

	private void initBrowserClient(String browserKey, BrowserClient browserClient) {
		browserClient.setId(browserKey);
		browserClient.setName(testName.getMethodName());
		browserClient.init();
		browserClient.injectKurentoTestJs();
	}

	@After
	public void teardownKurentoTest() {
		if (testScenario != null) {
			for (BrowserClient browserClient : testScenario.getBrowserMap().values()) {
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

	public void addBrowserClient(String browserKey, BrowserClient browserClient) {
		testScenario.getBrowserMap().put(browserKey, browserClient);
		initBrowserClient(browserKey, browserClient);
	}

	public E getBrowser(String browserKey) {
		return assertAndGetBrowser(browserKey);
	}

	public E getBrowser() {
		try {
			return assertAndGetBrowser(BrowserConfig.BROWSER);

		} catch (RuntimeException e) {
			if (testScenario.getBrowserMap().isEmpty()) {
				throw new RuntimeException("Empty test scenario: no available browser to run tests!");
			} else {
				String browserKey = testScenario.getBrowserMap().entrySet().iterator().next().getKey();
				log.debug(BrowserConfig.BROWSER + " is not registered in test scenarario, instead"
						+ " using first browser in the test scenario, i.e. " + browserKey);

				return getClient(browserKey);
			}
		}
	}

	public E getBrowser(int index) {
		return assertAndGetBrowser(BrowserConfig.BROWSER + index);
	}

	public E getPresenter() {
		return assertAndGetBrowser(BrowserConfig.PRESENTER);
	}

	public E getPresenter(int index) {
		return assertAndGetBrowser(BrowserConfig.PRESENTER + index);
	}

	public E getViewer() {
		return assertAndGetBrowser(BrowserConfig.VIEWER);
	}

	public E getViewer(int index) {
		return assertAndGetBrowser(BrowserConfig.VIEWER + index);
	}

	private E assertAndGetBrowser(String browserKey) {
		if (!testScenario.getBrowserMap().keySet().contains(browserKey)) {
			throw new RuntimeException(browserKey + " is not registered as browser in the test scenario");
		}
		return getClient(browserKey);
	}

	public synchronized E getClient(String browserKey) {
		E client;
		if (clients.containsKey(browserKey)) {
			client = clients.get(browserKey);
		} else {
			client = (E) createTestClient();
			client.setBrowserClient(testScenario.getBrowserMap().get(browserKey));
			clients.put(browserKey, client);
		}

		return client;
	}

	@SuppressWarnings("unchecked")
	protected E createTestClient() {

		Class<?> testClientClass = getParamType(this.getClass());

		try {
			return (E) testClientClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException("Exception creating an instance of class " + testClientClass.getName(), e);
		}
	}

	public static Class<?> getParamType(Class<?> testClass) {

		Type genericSuperclass = testClass.getGenericSuperclass();

		if (genericSuperclass != null) {

			if (genericSuperclass instanceof Class) {
				return getParamType((Class<?>) genericSuperclass);
			}

			ParameterizedType paramClass = (ParameterizedType) genericSuperclass;

			return (Class<?>) paramClass.getActualTypeArguments()[0];
		}

		throw new RuntimeException("Unable to obtain the type paramter of KurentoTest");
	}

	public void waitForHostIsReachable(URL url, int timeout) {
		long timeoutMillis = TimeUnit.MILLISECONDS.convert(timeout, TimeUnit.SECONDS);
		long endTimeMillis = System.currentTimeMillis() + timeoutMillis;

		log.debug("Waiting for {} to be reachable (timeout {} seconds)", url, timeout);

		try {
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			} };

			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			HostnameVerifier allHostsValid = new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

			int responseCode = 0;
			while (true) {
				try {
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					connection.setConnectTimeout((int) timeoutMillis);
					connection.setReadTimeout((int) timeoutMillis);
					connection.setRequestMethod("HEAD");
					responseCode = connection.getResponseCode();

					break;
				} catch (SSLHandshakeException | SocketException e) {
					log.warn("Error {} waiting URL {}, trying again in 1 second", e.getMessage(), url);
					// Polling to wait a consistent SSL state
					Thread.sleep(1000);
				}
				if (System.currentTimeMillis() > endTimeMillis) {
					break;
				}
			}

			if (responseCode != HttpURLConnection.HTTP_OK) {
				Assert.fail("URL " + url + " not reachable. Response code=" + responseCode);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("URL " + url + " not reachable in " + timeout + " seconds (" + e.getClass().getName() + ", "
					+ e.getMessage() + ")");
		}

		log.debug("URL {} already reachable", url);
	}

	public Map<String, E> getClients() {
		return clients;
	}

}
