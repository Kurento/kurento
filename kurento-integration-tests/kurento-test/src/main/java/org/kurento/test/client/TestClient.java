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
package org.kurento.test.client;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic client for tests using Kurento test infrastructure.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.1.0
 */
public class TestClient {

	public static Logger log = LoggerFactory.getLogger(TestClient.class);

	public BrowserClient browserClient;

	public TestClient() {
	}

	public TestClient(TestClient client) {
		this.browserClient = client.browserClient;
	}

	public BrowserClient getBrowserClient() {
		return browserClient;
	}

	public void setBrowserClient(BrowserClient browserClient) {
		this.browserClient = browserClient;
	}

	public void takeScreeshot(String file) throws IOException {
		File scrFile = ((TakesScreenshot) getBrowserClient().getDriver())
				.getScreenshotAs(OutputType.FILE);
		FileUtils.copyFile(scrFile, new File(file));
	}

}
