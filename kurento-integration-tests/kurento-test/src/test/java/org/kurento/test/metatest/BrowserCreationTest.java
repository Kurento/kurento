package org.kurento.test.metatest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kurento.commons.PropertiesManager;
import org.kurento.commons.testing.SystemTests;
import org.kurento.test.browser.Browser;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.internal.AbortableCountDownLatch;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.emory.mathcs.backport.java.util.Collections;

@Category(SystemTests.class)
public class BrowserCreationTest {

	private static final Logger log = LoggerFactory
			.getLogger(BrowserCreationTest.class);

	private static final int NUM_BROWSERS = PropertiesManager
			.getProperty("test.BrowserCreationTest.numBrowsers", 5);
	private static final int NUM_ITERATIONS = PropertiesManager
			.getProperty("test.BrowserCreationTest.numIterations", 3);

	@Test
	public void testParallelBrowser() throws InterruptedException, IOException {

		System.setProperty("test.selenium.record", "false");

		initTestFolder("testParallelBrowser");

		for (int i = 0; i < NUM_ITERATIONS; i++) {
			createParallelBrowsers(NUM_BROWSERS);
		}
	}

	@Test
	public void testSerialBrowser() throws InterruptedException, IOException {

		System.setProperty("test.selenium.record", "false");

		initTestFolder("testSerialBrowser");

		for (int i = 0; i < NUM_ITERATIONS; i++) {

			for (int j = 0; j < NUM_BROWSERS; j++) {

				log.info("Created browser {}-{}", i, j);

				Browser browser = new Browser.Builder()
						.scope(BrowserScope.DOCKER).build();

				browser.setId("browser_" + i + "_" + j);

				browser.init();

				browser.close();
			}
		}
	}

	private void createParallelBrowsers(int numBrowsers)
			throws InterruptedException {
		long startTime = System.currentTimeMillis();

		@SuppressWarnings("unchecked")
		final List<Browser> browsers = Collections
				.synchronizedList(new ArrayList<Browser>());

		ExecutorService executor = Executors.newFixedThreadPool(numBrowsers);

		try {

			final AbortableCountDownLatch latch = new AbortableCountDownLatch(
					numBrowsers);

			for (int i = 0; i < numBrowsers; i++) {

				final int numBrowser = i;

				executor.execute(new Runnable() {

					@Override
					public void run() {
						try {

							Browser browser = new Browser.Builder()
									.scope(BrowserScope.DOCKER).build();

							browsers.add(browser);

							browser.setId("browser" + numBrowser);

							browser.init();

							latch.countDown();

						} catch (Throwable t) {
							latch.abort(
									"Exception setting up test. A browser could not be initialised",
									t);
						}
					}
				});
			}

			latch.await();

			long creationTime = System.currentTimeMillis() - startTime;

			log.info(
					"----------------------------------------------------------------");

			log.info("All {} browsers started in {} millis", numBrowsers,
					creationTime);

			log.info(
					"----------------------------------------------------------------");

		} finally {

			log.info(
					"***************************************************************");

			startTime = System.currentTimeMillis();

			final AbortableCountDownLatch latch = new AbortableCountDownLatch(
					numBrowsers);

			for (final Browser browser : browsers) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						browser.close();
						latch.countDown();
					}
				});
			}

			executor.shutdown();
			executor.awaitTermination(10, TimeUnit.HOURS);

			latch.await();

			long destructionTime = System.currentTimeMillis() - startTime;

			log.info(
					"----------------------------------------------------------------");

			log.info("All {} browsers stopped in {} millis", numBrowsers,
					destructionTime);

			log.info(
					"----------------------------------------------------------------");
		}
	}

	private void initTestFolder(String testName) throws IOException {

		KurentoServicesTestHelper
				.setTestCaseName(this.getClass().getSimpleName());
		KurentoServicesTestHelper.setTestName(testName);

		log.info("Tests dir {}", KurentoServicesTestHelper.getTestDir());

		Path testFolder = Paths.get(KurentoServicesTestHelper.getTestDir(),
				"BrowserCreationTest");

		if (Files.exists(testFolder)) {
			log.debug("Deleting test folder {}", testFolder);
			FileUtils.forceDelete(testFolder.toFile());
		}

		log.debug("Creating test folder {}", testFolder);
		Files.createDirectories(testFolder);
	}

}
