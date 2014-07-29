/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
package com.kurento.test.selenium;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 * Selenium tests for basic KMF features; these tests uses Campus Party Handlers
 * (HTTP Player with JSON-RPC signaling protocol).
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.0
 */
@RunWith(Arquillian.class)
public class SeleniumBasicIT extends BaseSeleniumTst {

	@Before
	public void setup() {
		// In these tests it is not required waiting until the end of the video
		// to perform the required assessment
		setTimeout(10);
		setWaitEnd(false);
	}

	@Test
	public void testPlayerJsonRedirectChrome() throws Exception {
		seleniumTest(ChromeDriver.class, "player-json-redirect", "webm");
	}

	@Test
	public void testPlayerJsonRedirectFirefox() throws Exception {
		seleniumTest(FirefoxDriver.class, "player-json-redirect", "mov");
	}

	@Test
	public void testPlayerJsonTunnelChrome() throws Exception {
		seleniumTest(ChromeDriver.class, "player-json-tunnel", "3gp");
	}

	@Test
	public void testPlayerJsonTunnelFirefox() throws Exception {
		seleniumTest(FirefoxDriver.class, "player-json-tunnel", "ogv");
	}

	@Test
	public void testPlayerJsonFaceOverlayRedirectChrome() throws Exception {
		seleniumTest(ChromeDriver.class, "player-json-redirect", "face");
	}

	@Test
	public void testPlayerJsonFaceOverlayTunnelFirefox() throws Exception {
		seleniumTest(FirefoxDriver.class, "player-json-tunnel", "face");
	}

	public void testPlayerJsonZBar(Class<? extends WebDriver> driverClass,
			String handler) throws Exception {
		final String[] expectedEvents = {
				"{\"type\":\"CodeFound\",\"data\":\"MEBKM:URL:http\\\\://en.wikipedia.org/wiki/Main_Page;;\"}",
				"{\"type\":\"CodeFound\",\"data\":\"http://my-fashion.jp/t/47/\"}",
				"{\"type\":\"CodeFound\",\"data\":\"http://www.monocle.com\"}" };
		seleniumTest(driverClass, handler, "zbar", expectedEvents);
	}

	@Test
	public void testPlayerJsonZBarRedirectChrome() throws Exception {
		testPlayerJsonZBar(ChromeDriver.class, "player-json-redirect");
	}

	@Test
	public void testPlayerJsonZBarTunnelFirefox() throws Exception {
		testPlayerJsonZBar(FirefoxDriver.class, "player-json-redirect");
	}

	/**
	 * @since 4.1.1
	 */
	@Test
	public void testPlayerCrowdRedirectChrome() throws Exception {
		seleniumTest(ChromeDriver.class, "player-json-redirect", "crowd");
	}

	/**
	 * @since 4.1.1
	 */
	@Test
	public void testPlayerCrowdTunnelFirefox() throws Exception {
		seleniumTest(FirefoxDriver.class, "player-json-tunnel", "crowd");
	}

	/**
	 * @since 4.1.1
	 */
	@Test
	public void testPlayerRtspRedirectChrome() throws Exception {
		seleniumTest(ChromeDriver.class, "player-json-redirect", "rtsp");
	}

	/**
	 * @since 4.1.1
	 */
	@Test
	public void testPlayerRtspTunnelFirefox() throws Exception {
		seleniumTest(FirefoxDriver.class, "player-json-tunnel", "rtsp");
	}

	/**
	 * @since 4.1.1
	 */
	public void testPlayerJsonPlate(Class<? extends WebDriver> driverClass,
			String handler) throws Exception {
		final String[] expectedEvents = {
				"{\"type\":\"plate-detected\",\"data\":\"--2651DCL\"}",
				"{\"type\":\"plate-detected\",\"data\":\"-M9309WV-\"}",
				"{\"type\":\"plate-detected\",\"data\":\"--3882GKP\"}" };
		seleniumTest(driverClass, handler, "plate", expectedEvents);
	}

	/**
	 * @since 4.1.1
	 */
	@Test
	public void testPlayerJsonPlateRedirectChrome() throws Exception {
		testPlayerJsonPlate(ChromeDriver.class, "player-json-redirect");
	}

	/**
	 * @since 4.1.1
	 */
	@Test
	public void testPlayerJsonPlateTunnelFirefox() throws Exception {
		testPlayerJsonPlate(FirefoxDriver.class, "player-json-redirect");
	}

	/**
	 * @since 4.1.1
	 */
	public void testPlayerJsonPointer(Class<? extends WebDriver> driverClass,
			String handler) throws Exception {
		final String[] expectedEvents = { "{\"type\":\"WindowIn\",\"data\":\"goal\"}" };
		seleniumTest(driverClass, handler, "pointer", expectedEvents);
	}

	/**
	 * @since 4.1.1
	 */
	@Test
	public void testPlayerJsonPointerRedirectChrome() throws Exception {
		testPlayerJsonPointer(ChromeDriver.class, "player-json-redirect");
	}

	/**
	 * @since 4.1.1
	 */
	@Test
	public void testPlayerJsonPointerTunnelFirefox() throws Exception {
		testPlayerJsonPointer(FirefoxDriver.class, "player-json-redirect");
	}

}
