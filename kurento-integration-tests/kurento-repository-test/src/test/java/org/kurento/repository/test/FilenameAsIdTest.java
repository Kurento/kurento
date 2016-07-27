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
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.kurento.repository.Repository;
import org.kurento.repository.internal.repoimpl.mongo.MongoRepository;
import org.kurento.repository.test.util.BaseRepositoryTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import com.mongodb.util.JSON;

public class FilenameAsIdTest extends BaseRepositoryTest {

  private static final Logger log = LoggerFactory.getLogger(FilenameAsIdTest.class);

  @Test
  public void test() throws IOException {

    Repository repository = getRepository();

    if (repository instanceof MongoRepository) {

      MongoRepository mongoRepository = (MongoRepository) repository;

      GridFS gridFS = mongoRepository.getGridFS();

      GridFSInputFile file = gridFS.createFile(new File("test-files/sample.txt"));

      file.setId("sample.txt");

      file.save();

      List<GridFSDBFile> files = gridFS.find((DBObject) JSON.parse("{ _id : 'sample.txt' }"));

      assertNotNull(files);
      assertEquals(1, files.size());
    } else {
      log.debug("Repository is not MongoDB");
    }

  }

}
