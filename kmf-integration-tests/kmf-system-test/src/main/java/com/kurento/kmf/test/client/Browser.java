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
package com.kurento.kmf.test.client;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 * Browser to perform automated web testing with Selenium WebDriver.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public enum Browser {
	CHROME, CHROME_FOR_TEST, FIREFOX;

	public boolean getFlags() {
		switch (this) {
		case CHROME:
			return false;
		case FIREFOX:
			return false;
		case CHROME_FOR_TEST:
		default:
			return true;
		}
	}

	public Class<? extends WebDriver> getDriverClass() {
		switch (this) {
		case CHROME:
			return ChromeDriver.class;
		case FIREFOX:
			return FirefoxDriver.class;
		case CHROME_FOR_TEST:
		default:
			return ChromeDriver.class;
		}
	}

}
