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

package com.kurento.kmf.repository.internal.repoimpl.filesystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

public class ItemsMetadata {

	private Logger log = LoggerFactory.getLogger(ItemsMetadata.class);

	// TODO Avoid potential memory leaks using Google's MapMaker
	private ConcurrentMap<String, Map<String, String>> itemsMetadata;

	private File itemsMetadataFile;

	public ItemsMetadata(File itemsMetadataFile) {
		this.itemsMetadataFile = itemsMetadataFile;
		try {
			loadItemsMetadata();
		} catch (IOException e) {
			log.warn("Exception while loading items metadata", e);
		}
	}

	private void loadItemsMetadata() throws IOException {
		itemsMetadata = new ConcurrentHashMap<String, Map<String, String>>();
		DBObject contents = (DBObject) JSON.parse(loadFileAsString());
		if (contents != null) {
			for (String key : contents.keySet()) {
				try {
					DBObject metadata = (DBObject) contents.get(key);
					Map<String, String> map = new HashMap<String, String>();
					for (String metadataKey : metadata.keySet()) {
						map.put(metadataKey, metadata.get(metadataKey)
								.toString());
					}
					itemsMetadata.put(key, map);
				} catch (ClassCastException e) {
					log.warn("Attribute '" + key + "' should be an object");
				}
			}
		}
	}

	private String loadFileAsString() throws IOException {

		if (!itemsMetadataFile.exists()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		FileReader metadataFile = new FileReader(itemsMetadataFile);
		BufferedReader br = new BufferedReader(metadataFile);
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line).append("\n");
		}
		br.close();
		return sb.toString();
	}

	public synchronized void setMetadataForId(String id,
			Map<String, String> metadata) {
		itemsMetadata.put(id, metadata);
	}

	public synchronized Map<String, String> loadMetadata(String id) {
		Map<String, String> metadata = itemsMetadata.get(id);
		if (metadata == null) {
			metadata = new HashMap<String, String>();
			itemsMetadata.put(id, metadata);
		}
		return metadata;
	}

	public List<Entry<String, Map<String, String>>> findByAttValue(
			String attributeName, String value) {

		List<Entry<String, Map<String, String>>> list = new ArrayList<Map.Entry<String, Map<String, String>>>();

		for (Entry<String, Map<String, String>> item : itemsMetadata.entrySet()) {
			String attValue = item.getValue().get(attributeName);
			if (attValue != null && attValue.equals(value)) {
				list.add(item);
			}
		}

		return list;
	}

	public List<Entry<String, Map<String, String>>> findByAttRegex(
			String attributeName, String regex) {

		Pattern pattern = Pattern.compile(regex);

		List<Entry<String, Map<String, String>>> list = new ArrayList<Map.Entry<String, Map<String, String>>>();

		for (Entry<String, Map<String, String>> item : itemsMetadata.entrySet()) {
			String value = item.getValue().get(attributeName);
			if (value != null && pattern.matcher(value).matches()) {
				list.add(item);
			}
		}

		return list;
	}

	public void save() {

		try {
			if (!itemsMetadataFile.exists()) {
				itemsMetadataFile.getParentFile().mkdirs();
				itemsMetadataFile.createNewFile();
			}
			PrintWriter writer = new PrintWriter(itemsMetadataFile);
			String content = JSON.serialize(itemsMetadata);
			writer.print(content);
			writer.close();
		} catch (IOException e) {
			log.error("Exception writing metadata file", e);
		}
	}
}
