/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

public class MetaTest1Test extends KurentoClientBrowserTest<WebRtcTestPage> {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChrome();
  }

  @Test
  public void test() {

    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();

    // Check page loaded
    WebElement element =
        getPage().getBrowser().getWebDriver().findElement(By.cssSelector("#testTitle"));

    Assert.assertThat(element.getText(), IsEqual.equalTo("WebRTC test"));

    // Release Media Pipeline
    mp.release();
  }
}
