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

package org.kurento.test.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract tect service.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public abstract class TestService {

  public static Logger log = LoggerFactory.getLogger(TestService.class);

  public enum TestServiceScope {
    TEST, TESTCLASS, TESTSUITE, EXTERNAL;
  }

  public TestServiceScope scope = TestServiceScope.TEST;

  public abstract TestServiceScope getScope();

  public void start() {
    log.debug("[+] Starting {} (scope={})", this.getClass().getSimpleName(), getScope());
  }

  public void stop() {
    log.debug("[-] Stopping {} (scope={})", this.getClass().getSimpleName(), getScope());
  }

}
