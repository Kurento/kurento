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

import java.awt.Color;

import org.junit.Before;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Base for tests using kurento-client and Http Server.
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
@EnableAutoConfiguration
public class BrowserKurentoClientTest extends KurentoClientTest {

	public static final Color CHROME_VIDEOTEST_COLOR = new Color(0, 135, 0);

	@Before
	public void setupHttpServer() throws Exception {
		if (!this.getClass().isAnnotationPresent(WebAppConfiguration.class)) {
			KurentoServicesTestHelper
					.startHttpServer(BrowserKurentoClientTest.class);
		}
	}
}
