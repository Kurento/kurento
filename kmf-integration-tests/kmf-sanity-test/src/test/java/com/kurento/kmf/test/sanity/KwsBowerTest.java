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

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.kurento.kmf.test.Shell;

/**
 * Sanity test for KWS releases with Bower.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
public class KwsBowerTest extends KwsBase {

	@BeforeClass
	public static void runBower() throws IOException {
		Shell.runAndWait("sh", "-c", "bower install kws-media-api");
		Shell.runAndWait("sh", "-c", "bower install kws-utils");

		final String outputFolder = new ClassPathResource("static").getFile()
				.getAbsolutePath() + File.separator;

		Shell.runAndWait("sh", "-c", "cp -r bower_components/kws-utils/js "
				+ outputFolder);
		Shell.runAndWait("sh", "-c", "cp -r bower_components/kws-media-api/js "
				+ outputFolder);
	}

	public KwsBowerTest() {
		kwsUrl = getProperty("kws.release.url", "./");
	}

	@Test
	public void kwsBowerTest() {
		doTest();
	}

	@AfterClass
	public static void rmBower() {
		Shell.runAndWait("sh", "-c", "rm -rf bower_components");
	}

}
