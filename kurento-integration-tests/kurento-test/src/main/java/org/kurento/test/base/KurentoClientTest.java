/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

  public static @Service KmsService kms = new KmsService();
  public static @Service KmsService fakeKms = new FakeKmsService();

  protected KurentoClient kurentoClient;
  protected KurentoClient fakeKurentoClient;

  @Before
  public void setupKurentoClient() {
    kurentoClient = kms.getKurentoClient();
    fakeKurentoClient = fakeKms.getKurentoClient();
  }

  @After
  public void teardownKurentoClient() throws Exception {
    if (kurentoClient != null) {
      kurentoClient.destroy();
    }
    if (fakeKurentoClient != null) {
      fakeKurentoClient.destroy();
    }
  }

}
