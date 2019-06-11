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

package org.kurento.repository.internal.repoimpl.mongo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.PostConstruct;

import org.kurento.repository.DuplicateItemException;
import org.kurento.repository.RepositoryItem;
import org.kurento.repository.internal.http.RepositoryHttpManager;
import org.kurento.repository.internal.repoimpl.RepositoryWithHttp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import com.mongodb.util.JSON;

public class MongoRepository implements RepositoryWithHttp {

  private final Logger log = LoggerFactory.getLogger(MongoRepository.class);

  @Autowired
  private MongoTemplate mongoTemplate;

  private GridFS gridFS;

  @Autowired
  private RepositoryHttpManager httpManager;

  @PostConstruct
  private void postConstruct() {
	  // TODO: refactor GridFS API to SpringBoot 2
	  // gridFS = new GridFS(mongoTemplate.getDb());
  }

  // TODO Define ways to let users access to low level mongo backend. I prefer
  // using Spring with @Autowired, but can be useful to let users access
  // mongo from repository.
  public GridFS getGridFS() {
    return gridFS;
  }

  @Override
  public RepositoryItem findRepositoryItemById(String id) {

    List<GridFSDBFile> dbFiles = gridFS.find(id);

    if (dbFiles.size() > 0) {

      if (dbFiles.size() > 1) {
        log.warn("There are several files with the same " + "filename and should be only one");
      }

      return createRepositoryItem(dbFiles.get(0));
    }

    throw new NoSuchElementException("The repository item with id \"" + id + "\" does not exist");
  }
  //
  // private DBObject idQuery(String id) {
  // return new BasicDBObject("_id", id);
  // }

  private RepositoryItem createRepositoryItem(GridFSInputFile dbFile) {
    return new MongoRepositoryItem(this, dbFile);
  }

  private MongoRepositoryItem createRepositoryItem(GridFSDBFile dbFile) {

    MongoRepositoryItem item = new MongoRepositoryItem(this, dbFile);

    Map<String, String> metadata = new HashMap<>();
    DBObject object = dbFile.getMetaData();
    for (String key : object.keySet()) {
      metadata.put(key, object.get(key).toString());
    }
    item.setMetadata(metadata);
    return item;
  }

  @Override
  public RepositoryItem createRepositoryItem() {
    GridFSInputFile dbFile = gridFS.createFile();
    dbFile.setFilename(dbFile.getId().toString());
    return createRepositoryItem(dbFile);
  }

  @Override
  public RepositoryItem createRepositoryItem(String id) {

    // TODO The file is not written until outputstream is closed. There is a
    // potentially data race with this unique test
    if (!gridFS.find(id).isEmpty()) {
      throw new DuplicateItemException(id);
    }

    GridFSInputFile dbFile = gridFS.createFile(id);
    dbFile.setId(id);
    return createRepositoryItem(dbFile);
  }

  @Override
  public RepositoryHttpManager getRepositoryHttpManager() {
    return httpManager;
  }

  @Override
  public void remove(RepositoryItem item) {
    httpManager.disposeHttpRepoItemElemByItemId(item, "Repository Item removed");
    gridFS.remove(item.getId());
  }

  @Override
  public List<RepositoryItem> findRepositoryItemsByAttValue(String attributeName, String value) {

    String query = "{'metadata." + attributeName + "':'" + value + "'}";

    return findRepositoryItemsByQuery(query);
  }

  @Override
  public List<RepositoryItem> findRepositoryItemsByAttRegex(String attributeName, String regex) {

    String query = "{'metadata." + attributeName + "': { $regex : '" + regex + "'}}";

    return findRepositoryItemsByQuery(query);
  }

  private List<RepositoryItem> findRepositoryItemsByQuery(String query) {
    List<GridFSDBFile> files = gridFS.find((DBObject) JSON.parse(query));

    List<RepositoryItem> repositoryItems = new ArrayList<>();
    for (GridFSDBFile file : files) {
      repositoryItems.add(createRepositoryItem(file));
    }

    return repositoryItems;
  }

}
