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

package org.kurento.repository.test;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.kurento.repository.RepositoryHttpPlayer;
import org.kurento.repository.test.util.BaseRepositoryTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

public class TimeoutTests extends BaseRepositoryTest {

  private static final Logger log = LoggerFactory.getLogger(TimeoutTests.class);

  @Test
  public void playerAutoTerminationTest() throws Exception {

    String id = uploadFile(new File("test-files/sample.txt"));

    log.debug("File uploaded");

    RepositoryHttpPlayer player =
        getRepository().findRepositoryItemById(id).createRepositoryHttpPlayer();

    player.setAutoTerminationTimeout(1000);

    RestTemplate template = getRestTemplate();

    assertEquals(HttpStatus.OK,
        template.getForEntity(player.getURL(), byte[].class).getStatusCode());
    log.debug("Request 1 Passed");

    Thread.sleep(300);

    assertEquals(HttpStatus.OK,
        template.getForEntity(player.getURL(), byte[].class).getStatusCode());
    log.debug("Request 2 Passed");

    Thread.sleep(1500);

    assertEquals(HttpStatus.NOT_FOUND,
        template.getForEntity(player.getURL(), byte[].class).getStatusCode());
    log.debug("Request 3 Passed");

  }

}