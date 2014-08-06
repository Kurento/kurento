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

package org.kurento.repository.internal.repoimpl.filesystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.kurento.common.exception.KurentoException;
import org.kurento.repository.DuplicateItemException;
import org.kurento.repository.RepositoryApiConfiguration;
import org.kurento.repository.RepositoryItem;
import org.kurento.repository.internal.http.RepositoryHttpManager;
import org.kurento.repository.internal.repoimpl.RepositoryWithHttp;

@Component
public class FileSystemRepository implements RepositoryWithHttp {

	private static final Logger log = LoggerFactory
			.getLogger(FileSystemRepository.class);

	private static final String ITEMS_METADATA_FILE_PATH = "metadata/metadata.json";

	@Autowired
	private RepositoryApiConfiguration config;

	private File baseFolder;

	private int nextFreeFileNumber = 1;

	private ItemsMetadata metadata;

	@Autowired
	private RepositoryHttpManager httpManager;

	@PostConstruct
	public void init() {
		baseFolder = new File(config.getFileSystemFolder());
		checkFolder(baseFolder);
		calculateNextId();
		metadata = new ItemsMetadata(new File(baseFolder,
				ITEMS_METADATA_FILE_PATH));
	}

	@PreDestroy
	public void close() {
		log.debug("Closing file system repository");
		this.metadata.save();
	}

	private synchronized String calculateNextId() {

		// Very naive unique identifier system

		while (true) {
			File file = getFileForId(Integer.toString(nextFreeFileNumber));
			if (file.exists()) {
				nextFreeFileNumber++;
			} else {
				return Integer.toString(nextFreeFileNumber);
			}
		}
	}

	private void checkFolder(File folder) {
		if (folder.exists() && !folder.isDirectory()) {
			throw new IllegalArgumentException("The specified \""
					+ folder.getAbsolutePath() + "\" is not a valid folder");
		}

		if (!folder.exists()) {
			boolean created = folder.mkdirs();
			if (!created) {
				throw new IllegalArgumentException("Error while creating \""
						+ folder.getAbsolutePath() + "\" folder");
			}
		}

	}

	@Override
	public RepositoryItem findRepositoryItemById(String id) {

		File file = getFileForId(id);
		if (!file.exists()) {
			throw new NoSuchElementException("The repository item with id \""
					+ id + "\" does not exist");
		}

		return new FileRepositoryItem(this, file, id, metadata.loadMetadata(id));
	}

	@Override
	public RepositoryItem createRepositoryItem() {
		String id = calculateNextId();
		return new FileRepositoryItem(this, getFileForId(id), id,
				metadata.loadMetadata(id));
	}

	@Override
	public RepositoryItem createRepositoryItem(String id) {

		File file = getFileForId(id);

		if (file.exists()) {
			throw new DuplicateItemException(id);
		}

		return new FileRepositoryItem(this, file, id, metadata.loadMetadata(id));
	}

	@Override
	public List<RepositoryItem> findRepositoryItemsByAttValue(
			String attributeName, String value) {
		return createItemsForIds(metadata.findByAttValue(attributeName, value));
	}

	@Override
	public List<RepositoryItem> findRepositoryItemsByAttRegex(
			String attributeName, String regex) {
		return createItemsForIds(metadata.findByAttRegex(attributeName, regex));
	}

	private List<RepositoryItem> createItemsForIds(
			List<Entry<String, Map<String, String>>> itemsInfo) {
		List<RepositoryItem> items = new ArrayList<>();
		for (Entry<String, Map<String, String>> itemInfo : itemsInfo) {
			String id = itemInfo.getKey();
			items.add(new FileRepositoryItem(this, getFileForId(id), id,
					itemInfo.getValue()));
		}
		return items;
	}

	private File getFileForId(String id) {
		return new File(baseFolder, id);
	}

	@Override
	public RepositoryHttpManager getRepositoryHttpManager() {
		return httpManager;
	}

	@Override
	public void remove(RepositoryItem item) {
		FileRepositoryItem fileItem = (FileRepositoryItem) item;
		httpManager.disposeHttpRepoItemElemByItemId(item,
				"Repository Item removed");
		File file = fileItem.getFile();
		boolean success = file.delete();
		if (!success) {
			throw new KurentoException("The file can't be deleted");
		}
	}

	public void setMetadataForItem(FileRepositoryItem fileRepositoryItem,
			Map<String, String> metadata) {
		this.metadata.setMetadataForId(fileRepositoryItem.getId(), metadata);
	}
}
