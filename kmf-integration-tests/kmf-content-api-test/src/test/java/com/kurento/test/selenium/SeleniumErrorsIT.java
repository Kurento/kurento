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
package com.kurento.test.selenium;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.chrome.ChromeDriver;

/**
 * Selenium tests for assessment of error/exception situations.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.1.1
 */
@RunWith(Arquillian.class)
public class SeleniumErrorsIT extends BaseSeleniumTst {

	final String ERROR = "end with error ";

	@Test
	public void testExceptionChrome() throws Exception {
		setExpectedStatus(ERROR + "Custom exception in handler");
		seleniumTest(ChromeDriver.class, "playerErrors/exception", null, null,
				null, null);
	}

	@Test
	public void testTerminatioChrome() throws Exception {
		setExpectedStatus(ERROR + "Custom session termination in handler");
		seleniumTest(ChromeDriver.class, "playerErrors/terminate", null, null,
				null, null);
	}

}
