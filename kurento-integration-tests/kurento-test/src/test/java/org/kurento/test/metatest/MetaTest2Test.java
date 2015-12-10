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
package org.kurento.test.metatest;

import java.util.Collection;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.test.base.KurentoClientBrowserTest;
import org.kurento.test.browser.WebRtcTestPage;
import org.kurento.test.config.TestScenario;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 6.1.1
 */

public class MetaTest2Test extends KurentoClientBrowserTest<WebRtcTestPage> {

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.localChrome();
	}

	@Test
	public void test() {

		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();

		// Check page loaded
		WebElement element = getPage().getBrowser().getWebDriver()
				.findElement(By.cssSelector("#testTitle"));

		Assert.assertThat(element.getText(), IsEqual.equalTo("WebRTC test"));

		// Release Media Pipeline
		mp.release();
	}
}
