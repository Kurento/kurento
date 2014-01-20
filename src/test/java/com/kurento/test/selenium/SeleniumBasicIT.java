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
	public void testPlayerJsonJackVaderRedirectChrome() throws Exception {
		seleniumTest(ChromeDriver.class, "player-json-redirect", "jack");
	}

	@Test
	public void testPlayerJsonJackVaderTunnelFirefox() throws Exception {
		seleniumTest(FirefoxDriver.class, "player-json-tunnel", "jack");
	}

	public void testPlayerJsonZBar(Class<? extends WebDriver> driverClass,
			String handler) throws Exception {
		final String[] expectedEvents = {
				"{\"type\":\"CodeFound\",\"data\":\"MEBKM:URL:http\\\\://en.wikipedia.org/wiki/Main_Page;;\"}",
				"{\"type\":\"CodeFound\",\"data\":\"http://my-fashion.jp/t/47/\"}",
				"{\"type\":\"CodeFound\",\"data\":\"http://www.monocle.com\"}"
		// ,"{\"type\":\"CodeFound\",\"data\":\"http://ganso-flea.com \\r\\n\\r\\nMEBKM:TITLE:FLEA CIRCUS;URL:http\\\\://ganso-flea.com;;\"}",
		// "{\"type\":\"CodeFound\",\"data\":\"http://game.nwa.com/s2/\"}"
		};
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

}
