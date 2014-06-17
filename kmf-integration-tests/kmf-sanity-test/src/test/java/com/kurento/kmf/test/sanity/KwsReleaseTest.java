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
package com.kurento.kmf.test.sanity;

import static com.kurento.kmf.common.PropertiesManager.getProperty;

import org.junit.Test;

/**
 * Sanity test for KWS releases.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
public class KwsReleaseTest extends KwsBase {

	public KwsReleaseTest() {
		kwsUrl = getProperty("kws.release.url", DEFAULT_KWS_URL);
		if (!kwsUrl.endsWith("/")) {
			kwsUrl += "/";
		}
	}

	@Test
	public void kwsReleaseTest() {
		doTest();
	}

}
