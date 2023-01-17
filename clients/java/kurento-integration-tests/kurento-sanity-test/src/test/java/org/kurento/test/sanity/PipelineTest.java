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

package org.kurento.test.sanity;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kurento.client.MediaPipeline;
import org.kurento.commons.testing.SanityTests;
import org.kurento.test.base.KurentoClientBrowserTest;
import org.kurento.test.browser.WebPage;

/**
 * Sanity test of a Media Pipeline creation and release.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
@Category(SanityTests.class)
public class PipelineTest extends KurentoClientBrowserTest<WebPage> {

  // TODO: this test should extend KurentoClientTest

  @Test
  public void basicPipelineTest() {
    MediaPipeline mediaPipeline = kurentoClient.createMediaPipeline();
    Assert.assertNotNull("Error: MediaPipeline is null", mediaPipeline);
    mediaPipeline.release();
  }

}
