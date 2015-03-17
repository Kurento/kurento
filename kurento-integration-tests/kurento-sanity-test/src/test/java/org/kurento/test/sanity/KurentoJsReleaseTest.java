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
package org.kurento.test.sanity;

import static org.kurento.commons.PropertiesManager.getProperty;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.test.config.TestScenario;

/**
 * Sanity test for kurento-js releases.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
public class KurentoJsReleaseTest extends KurentoJsBase {

	public KurentoJsReleaseTest() {
		super(new TestScenario());

		kurentoUrl = getProperty("kurento.release.url", DEFAULT_KURENTO_JS_URL);
		log.debug("kurentoUrl = {}", kurentoUrl);
		if (!kurentoUrl.endsWith("/")) {
			kurentoUrl += "/";
		}
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] { {} });
	}

	@Test
	public void kurentoJsReleaseTest() {
		doTest();
	}

}
