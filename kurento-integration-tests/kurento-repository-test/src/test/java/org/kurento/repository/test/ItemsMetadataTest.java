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
import java.io.IOException;
import java.util.Map;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kurento.commons.testing.RepositoryApiTests;
import org.kurento.repository.internal.repoimpl.filesystem.ItemsMetadata;

@Category(RepositoryApiTests.class)
public class ItemsMetadataTest {

  @Test
  public void test() throws IOException {

    File tempFile = File.createTempFile("metadata", "");

    ItemsMetadata itemsMetadata = new ItemsMetadata(tempFile);

    for (int i = 0; i < 10; i++) {
      Map<String, String> md1 = itemsMetadata.loadMetadata("o" + i);
      md1.put("differentAtt", "value" + i);
      md1.put("sameAtt", "value");
    }

    itemsMetadata.save();

    itemsMetadata = new ItemsMetadata(tempFile);

    assertEquals(10, itemsMetadata.findByAttValue("sameAtt", "value").size());
    assertEquals(1, itemsMetadata.findByAttValue("differentAtt", "value1").size());
    assertEquals(10, itemsMetadata.findByAttRegex("differentAtt", "value.*").size());

  }

}
