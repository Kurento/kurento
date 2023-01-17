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

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.kurento.repository.RepositoryHttpRecorder;
import org.kurento.repository.RepositoryItem;
import org.kurento.repository.test.util.BaseRepositoryTest;
import org.kurento.repository.test.util.TestUtils;

public class RepositoryItemsTest extends BaseRepositoryTest {

  @Test
  public void testFileUpload() throws Exception {
    uploadFile(new File("test-files/sample.txt"));
  }

  @Test
  public void testFileUploadWithPOSTAndDownload() throws Exception {

    RepositoryItem repositoryItem = getRepository().createRepositoryItem();

    String id = repositoryItem.getId();

    File fileToUpload = new File("test-files/sample.txt");
    RepositoryHttpRecorder recorder = repositoryItem.createRepositoryHttpRecorder();

    uploadFileWithPOST(recorder.getURL(), fileToUpload);

    recorder.stop();

    RepositoryItem newRepositoryItem = getRepository().findRepositoryItemById(id);

    File downloadedFile = new File("test-files/tmp/" + id);
    downloadFromURL(newRepositoryItem.createRepositoryHttpPlayer().getURL(), downloadedFile);

    assertTrue(TestUtils.equalFiles(fileToUpload, downloadedFile));
  }

}