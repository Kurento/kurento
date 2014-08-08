/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

package org.kurento.repository.internal.repoimpl.mongo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.kurento.repository.DuplicateItemException;
import org.kurento.repository.RepositoryItem;
import org.kurento.repository.internal.http.RepositoryHttpManager;
import org.kurento.repository.internal.repoimpl.RepositoryWithHttp;

import com.mongodb.BasicDBObject;
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
		gridFS = new GridFS(mongoTemplate.getDb());
	}

	// TODO Define ways to let users access to low level mongo backend. I prefer
	// using Spring with @Autowired, but can be useful to let users access
	// mongo from repository.
	public GridFS getGridFS() {
		return gridFS;
	}

	@Override
	public RepositoryItem findRepositoryItemById(String id) {

		List<GridFSDBFile> dbFiles = gridFS.find(idQuery(id));

		if (dbFiles.size() > 0) {

			if (dbFiles.size() > 1) {
				log.warn("There are several files with the same "
						+ "filename and should be only one");
			}

			return createRepositoryItem(dbFiles.get(0));
		}

		throw new NoSuchElementException("The repository item with id \"" + id
				+ "\" does not exist");
	}

	private DBObject idQuery(String id) {
		return new BasicDBObject("_id", id);
	}

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
		if (!gridFS.find(idQuery(id)).isEmpty()) {
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
		httpManager.disposeHttpRepoItemElemByItemId(item,
				"Repository Item removed");
		gridFS.remove(idQuery(item.getId()));
	}

	@Override
	public List<RepositoryItem> findRepositoryItemsByAttValue(
			String attributeName, String value) {

		String query = "{'metadata." + attributeName + "':'" + value + "'}";

		return findRepositoryItemsByQuery(query);
	}

	@Override
	public List<RepositoryItem> findRepositoryItemsByAttRegex(
			String attributeName, String regex) {

		String query = "{'metadata." + attributeName + "': { $regex : '"
				+ regex + "'}}";

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
