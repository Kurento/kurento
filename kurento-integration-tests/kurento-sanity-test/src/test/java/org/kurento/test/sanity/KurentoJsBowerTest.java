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

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kurento.test.Shell;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.springframework.core.io.ClassPathResource;

/**
 * Sanity test for kurento-js releases with Bower.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
public class KurentoJsBowerTest extends KurentoJsBase {

	@BeforeClass
	public static void runBower() throws IOException {
		String bowerClientTag = KurentoServicesTestHelper
				.getBowerKurentoClientTag();
		String bowerUtilsTag = KurentoServicesTestHelper
				.getBowerKurentoUtilsTag();
		if (!bowerClientTag.isEmpty()) {
			bowerClientTag = "#" + bowerClientTag;
		}
		if (!bowerUtilsTag.isEmpty()) {
			bowerUtilsTag = "#" + bowerUtilsTag;
		}

		log.debug("Using bower to download kurento-client"
				+ bowerClientTag
				+ "\n"
				+ Shell.runAndWait("sh", "-c", "bower install kurento-client"
						+ bowerClientTag));
		log.debug("Using bower to download kurento-utils"
				+ bowerUtilsTag
				+ "\n"
				+ Shell.runAndWait("sh", "-c", "bower install kurento-utils"
						+ bowerUtilsTag));

		final String outputFolder = new ClassPathResource("static").getFile()
				.getAbsolutePath() + File.separator;

		log.debug("Copying files from bower_components/kurento-utils/js to "
				+ outputFolder
				+ Shell.runAndWait("sh", "-c",
						"cp -r bower_components/kurento-utils/js "
								+ outputFolder));
		log.debug("Copying files from bower_components/kurento-client/js to "
				+ outputFolder
				+ Shell.runAndWait("sh", "-c",
						"cp -r bower_components/kurento-client/js "
								+ outputFolder));
	}

	public KurentoJsBowerTest() {
		kurentoUrl = "./";
	}

	@Test
	public void kurentoJsBowerTest() {
		doTest();
	}

	@AfterClass
	public static void rmBower() {
		Shell.runAndWait("sh", "-c", "rm -rf bower_components");
	}

}
