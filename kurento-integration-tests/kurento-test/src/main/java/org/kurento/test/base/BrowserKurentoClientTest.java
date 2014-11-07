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

import org.junit.runner.RunWith;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Base for tests using kurento-client and Http Server.
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { BrowserKurentoClientTest.class })
@WebAppConfiguration
@IntegrationTest("server.port:"
		+ KurentoServicesTestHelper.APP_HTTP_PORT_DEFAULT)
@ComponentScan
@EnableAutoConfiguration
public class BrowserKurentoClientTest extends KurentoClientTest {

	public static final Color CHROME_VIDEOTEST_COLOR = new Color(0, 135, 0);

	public static void main(String[] args) throws Exception {
		new SpringApplication(BrowserKurentoClientTest.class).run(args);
	}
}
