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

import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.test.base.KurentoClientBrowserTest;
import org.kurento.test.browser.WebRtcTestPage;
import org.kurento.test.config.TestScenario;

/**
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 6.1.1
 */

public class MetaTestMountedVolumeTest extends KurentoClientBrowserTest<WebRtcTestPage> {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChrome();
  }

  @Test
  public void test() throws InterruptedException {

    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();

    String videoPath = "file://" + getTestFilesDiskPath() + "/video/filter/barcodes.webm";

    PlayerEndpoint p = new PlayerEndpoint.Builder(mp, videoPath).build();

    final CountDownLatch latch = new CountDownLatch(1);

    p.addErrorListener(new EventListener<ErrorEvent>() {
      @Override
      public void onEvent(ErrorEvent event) {
        log.warn("Error un player: " + event.getDescription());
        latch.countDown();
      }
    });

    p.play();

    if (latch.await(5, TimeUnit.SECONDS)) {
      fail("Player error");
    }

    // Release Media Pipeline
    mp.release();
  }
}
