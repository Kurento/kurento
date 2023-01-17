/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

package org.kurento.test.base;

import org.junit.After;
import org.junit.Before;
import org.kurento.client.KurentoClient;
import org.kurento.test.services.FakeKmsService;
import org.kurento.test.services.KmsService;
import org.kurento.test.services.Service;

/**
 * Base for tests using kurento-client.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 6.1.1
 */
public class KurentoClientTest extends KurentoTest {
  @Service
  public static KmsService kms = new KmsService();
  @Service
  public static KmsService fakeKms = new FakeKmsService();

  protected KurentoClient kurentoClient;
  protected KurentoClient fakeKurentoClient;

  @Before
  public void setupKurentoClient() {
    kurentoClient = kms.getKurentoClient();
  }

  @After
  public void teardownKurentoClient() throws Exception {
    if (kurentoClient != null) {
      kurentoClient.destroy();
    }
  }

}
