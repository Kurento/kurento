/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

package org.kurento.client.test.util;

import org.junit.After;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.kurento.client.MediaPipeline;
import org.kurento.client.test.ApiBase;
import org.kurento.commons.testing.KurentoClientTests;

@Category(KurentoClientTests.class)
public abstract class MediaPipelineBaseTest extends ApiBase {

  protected MediaPipeline pipeline;

  @Before
  public void setupPipeline() {
    pipeline = kurentoClient.createMediaPipeline();
  }

  @After
  public void teardownPipeline() {
    if (pipeline != null) {
      pipeline.release();
    }
  }
}
