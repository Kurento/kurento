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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.kurento.repository.DuplicateItemException;
import org.kurento.repository.Repository;
import org.kurento.repository.RepositoryItem;
import org.kurento.repository.test.util.BaseRepositoryTest;

public class ItemManagmentTest extends BaseRepositoryTest {

  @Test(expected = DuplicateItemException.class)
  public void duplicateTest() throws IOException {

    Repository repository = getRepository();

    RepositoryItem item = repository.createRepositoryItem("file1");
    item.createOutputStreamToWrite().close();

    RepositoryItem item2 = repository.createRepositoryItem("file1");
    item2.createOutputStreamToWrite().close();

  }

  @Test
  public void metadataTest() throws IOException {

    Repository repository = getRepository();

    for (int i = 0; i < 10; i++) {
      try {
        RepositoryItem item = repository.findRepositoryItemById("File" + i + ".txt");
        repository.remove(item);
      } catch (NoSuchElementException e) {
        // Do nothing if repository item doesn't exist
      }
    }

    for (int i = 0; i < 10; i++) {

      RepositoryItem item = repository.createRepositoryItem("File" + i + ".txt");
      item.putMetadataEntry("numFile", Integer.toString(i));
      item.putMetadataEntry("att", "value");
      item.putMetadataEntry("regexAtt", "token" + Integer.toString(i));
      OutputStream os = item.createOutputStreamToWrite();
      os.write(0);
      os.close();

    }

    for (int i = 0; i < 10; i++) {

      try {
        RepositoryItem item = repository.findRepositoryItemById("File" + i + ".txt");

        String numString = item.getMetadata().get("numFile");
        assertEquals(numString, Integer.toString(i));

        assertEquals(item.getMetadata().get("att"), "value");

      } catch (NoSuchElementException e) {
        fail("Element 'File" + i + ".txt' doesn't exist");
      }

    }

    List<RepositoryItem> items = repository.findRepositoryItemsByAttValue("att", "value");

    assertEquals("Found different items than expected", 10, items.size());

    items = repository.findRepositoryItemsByAttRegex("regexAtt", "token.*");

    assertEquals("Found different items than expected", 10, items.size());

  }

}
