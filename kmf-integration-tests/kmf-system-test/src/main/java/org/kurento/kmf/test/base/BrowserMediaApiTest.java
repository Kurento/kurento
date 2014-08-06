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

package org.kurento.kmf.test.base;

import org.junit.Before;
import org.junit.experimental.categories.Category;

import org.kurento.kmf.commons.tests.SystemMediaApiTests;
import org.kurento.kmf.test.services.KurentoServicesTestHelper;

/**
 * Base for tests using kmf-media-api and Jetty Http Server.
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
@Category(SystemMediaApiTests.class)
public class BrowserMediaApiTest extends MediaApiTest {

	@Before
	public void setupHttpServer() throws Exception {

		KurentoServicesTestHelper.startHttpServer();
	}

}
