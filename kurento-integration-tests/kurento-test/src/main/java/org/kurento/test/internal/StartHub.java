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
package org.kurento.test.internal;

import static org.kurento.commons.PropertiesManager.getProperty;

import org.kurento.test.grid.GridHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal utility for starting a Selenium Grid Hub (for manual testing/debug
 * purposes).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class StartHub {

	public Logger log = LoggerFactory.getLogger(StartHub.class);

	public static final String SELENIUM_HUB_PORT_PROPERTY = "selenium.hub.port";
	public static final int SELENIUM_HUB_PORT_DEFAULT = 4444;

	public static final String SELENIUM_HUB_HOST_PROPERTY = "selenium.hub.host";
	public static final String SELENIUM_HUB_HOST_DEFAULT = "127.0.0.1";

	public static void main(String[] args) throws Exception {
		String hubAddress = getProperty(SELENIUM_HUB_HOST_PROPERTY,
				SELENIUM_HUB_HOST_DEFAULT);
		int hubPort = getProperty(SELENIUM_HUB_PORT_PROPERTY,
				SELENIUM_HUB_PORT_DEFAULT);

		GridHub seleniumGridHub = new GridHub(hubAddress,
				hubPort);
		seleniumGridHub.start();
	}
}
