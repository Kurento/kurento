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

package org.kurento.repository;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import org.kurento.repository.RepositoryItem.State;
import org.kurento.repository.service.pojo.RepositoryItemPlayer;
import org.kurento.repository.service.pojo.RepositoryItemRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service component that exposes a simpler Java API for the Kurento Repository.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
@Service
public class RepositoryService {

	private static final Logger log = LoggerFactory
			.getLogger(RepositoryService.class);

	@Autowired
	private Repository repository;

	/**
	 * Creates a new repository item with the provided metadata and its
	 * associated recorder endpoint.
	 *
	 * @param metadata
	 *            key-value pairs, can be null
	 * @return a {@link RepositoryItemRecorder} containing the item's id and the
	 *         recorder URL
	 */
	public RepositoryItemRecorder createRepositoryItem(Map<String, String> metadata) {
		RepositoryItem item = repository.createRepositoryItem();
		if (metadata != null)
			item.setMetadata(metadata);
		RepositoryItemRecorder itemRec = new RepositoryItemRecorder();
		itemRec.setId(item.getId());
		itemRec.setUrl(getEndpointUrl(item, false, "Upload"));
		return itemRec;
	}

	/**
	 * Removes the repository item associated to the provided id.
	 * 
	 * @param itemId
	 *            the id of an existing repository item
	 * @throws ItemNotFoundException
	 */
	public void removeRepositoryItem(String itemId)
			throws ItemNotFoundException {
		repository.remove(findRepositoryItemById(itemId));
	}

	/**
	 * Obtains a new endpoint for reading (playing multimedia) from the
	 * repository item.
	 *
	 * @param itemId
	 *            the id of an existing repository item
	 * @return the URL of the reading endpoint
	 * @throws ItemNotFoundException
	 */
	public RepositoryItemPlayer getReadEndpoint(String itemId)
			throws ItemNotFoundException {
		RepositoryItemPlayer itemPlay = new RepositoryItemPlayer();
		itemPlay.setId(itemId);
		itemPlay.setUrl(getEndpointUrl(itemId, true, "Download"));
		return itemPlay;
	}

	/**
	 * Searches for repository items by each pair of attributes and their
	 * values. The values can be regular expressions (use the flag, Luke).
	 * 
	 * @param metadata
	 *            pairs of attributes and their values (can be regexes)
	 * @param regex
	 *            if true, will activate search by attribute regex
	 * @return a {@link Set}&lt;{@link String}&gt; with identifiers of the
	 *         repository items that were found
	 */
	public Set<String> findItems(Map<String, String> metadata,
			boolean regex) {
		Set<String> itemIds = new HashSet<String>();
		for (Entry<String, String> data : metadata.entrySet()) {
			List<RepositoryItem> foundItems = null;
			if (regex)
				foundItems = repository.findRepositoryItemsByAttRegex(
						data.getKey(), data.getValue());
			else
				foundItems = repository.findRepositoryItemsByAttValue(
						data.getKey(), data.getValue());
			if (foundItems != null)
				for (RepositoryItem item : foundItems)
					itemIds.add(item.getId());
		}
		return itemIds;
	}

	/**
	 * Returns the metadata from a repository item.
	 * 
	 * @param itemId
	 *            the id of an existing repository item
	 * @return the metadata map
	 * @throws ItemNotFoundException
	 */
	public Map<String, String> getRepositoryItemMetadata(String itemId)
			throws ItemNotFoundException {
		return findRepositoryItemById(itemId).getMetadata();
	}

	/**
	 * Replaces the metadata of a repository item.
	 * 
	 * @param itemId
	 *            the id of an existing repository item
	 * @param metadata
	 *            the new metadata
	 * @throws ItemNotFoundException
	 */
	public void setRepositoryItemMetadata(String itemId,
			Map<String, String> metadata) throws ItemNotFoundException {
		RepositoryItem item = findRepositoryItemById(itemId);
		Map<String, String> oldMetadata = item.getMetadata();
		item.setMetadata(metadata);
		log.info("Current metadata: {} - updated metadata: {}", oldMetadata,
				item.getMetadata());
	}

	/**
	 * Used to obtain the URL of a [play|rec] Http endpoint. Should be used only
	 * if the item'state is {@link State#STORED}, otherwise the search for the
	 * item will fail.
	 * 
	 * @param itemId
	 *            the id of an existing repository item
	 * @param toRead
	 *            if true, the endpoint will be for playing/reading, otherwise
	 *            for recording/writing
	 * @param action
	 *            a message to be used when logging the various events triggered
	 *            during the endpoint's lifetime (should be "Upload"|"Download")
	 * @return a public URL
	 * @throws ItemNotFoundException
	 */
	private String getEndpointUrl(String itemId, boolean toRead,
			final String action) throws ItemNotFoundException {
		return getEndpointUrl(findRepositoryItemById(itemId),
				toRead, action);
	}

	/**
	 * Used to obtain the URL of a [play|rec] Http endpoint.
	 * 
	 * @param item
	 *            an existing repository item
	 * @param toRead
	 *            if true, the endpoint will be for playing/reading, otherwise
	 *            for recording/writing
	 * @param action
	 *            a message to be used when logging the various events triggered
	 *            during the endpoint's lifetime (should be "Upload"|"Download")
	 * @return a public URL
	 */
	private String getEndpointUrl(RepositoryItem item, boolean toRead,
			final String action) {
		RepositoryHttpEndpoint endpoint = null;
		String type = null;
		if (toRead) {
			endpoint = item.createRepositoryHttpPlayer();
			type = "player";
		} else {
			endpoint = item.createRepositoryHttpRecorder();
			type = "recorder";
		}
		log.debug("Created {} for repo item #{}\n\turl={}", type, item.getId(),
				endpoint.getURL());

		endpoint.addSessionStartedListener(new RepositoryHttpEventListener<HttpSessionStartedEvent>() {
			@Override
			public void onEvent(HttpSessionStartedEvent event) {
				log.debug("{} started on repo item #{}", action, event
						.getSource().getRepositoryItem().getId());
			}
		});

		endpoint.addSessionTerminatedListener(new RepositoryHttpEventListener<HttpSessionTerminatedEvent>() {
			@Override
			public void onEvent(HttpSessionTerminatedEvent event) {
				log.debug("{} terminated on repo item #{}", action, event
						.getSource().getRepositoryItem().getId());
			}
		});

		endpoint.addSessionErrorListener(new RepositoryHttpEventListener<HttpSessionErrorEvent>() {

			@Override
			public void onEvent(HttpSessionErrorEvent event) {
				log.warn("{} error on repo item #{}: {}", action, event
						.getSource().getRepositoryItem().getId(),
						event.getDescription(), event.getCause());
			}
		});

		return endpoint.getURL();
	}

	/**
	 * Wrapper for {@link Repository#findRepositoryItemById(String)} that throws
	 * a checked exception when the item is not found.
	 * 
	 * @param itemId
	 *            the id of a repository item
	 * @return the found object
	 * @throws ItemNotFoundException
	 *             when there's no instance with the provided id
	 */
	private RepositoryItem findRepositoryItemById(String itemId)
			throws ItemNotFoundException {
		try {
			return repository.findRepositoryItemById(itemId);
		} catch (NoSuchElementException e) {
			log.debug("Provided id is not valid", e);
			throw new ItemNotFoundException(e.getMessage());
		}
	}
}
