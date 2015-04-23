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

import java.util.Map;

import org.kurento.commons.exception.KurentoException;
import org.kurento.repository.service.pojo.RepositoryItemStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RepositoryService {

	private static final Logger log = LoggerFactory
			.getLogger(RepositoryService.class);

	@Autowired
	private Repository repository;

	/**
	 * Creates a new repository item with the provided metadata and its
	 * associated store endpoint (recorder).
	 *
	 * @param metadata
	 *            key-value pairs, can be null
	 * @return a {@link RepositoryItemStore} containing the item's id and the
	 *         recorder URL
	 */
	public RepositoryItemStore createRepositoryItem(Map<String, String> metadata) {
		RepositoryItem item = repository.createRepositoryItem();
		if (metadata != null) {
			item.setMetadata(metadata);
		}
		log.info("New repository item #{} - metadata: {}", item.getId(),
				item.getMetadata());
		RepositoryItemStore itemStore = new RepositoryItemStore();
		itemStore.setId(item.getId());
		itemStore.setUrl(getWriteEndpoint(item));
		return itemStore;
	}

	/**
	 * Obtains a new endpoint for reading on the repository item.
	 *
	 * @param item
	 *            an existing repository item
	 * @return the URL of the reading endpoint
	 */
	public String getReadEndpoint(RepositoryItem item) {
		return getEndpoint(item, true, "Download");
	}

	/**
	 * Obtains a new endpoint for reading on the repository item.
	 *
	 * @param itemId
	 *            the id of an existing repository item
	 * @return the URL of the reading endpoint
	 */
	public String getReadEndpoint(String itemId) {
		return getEndpoint(itemId, true, "Download");
	}

	/**
	 * Obtains a new endpoint for writing on the repository item.
	 *
	 * @param an
	 *            existing repository item
	 * @return the URL of the writing endpoint
	 */
	public String getWriteEndpoint(RepositoryItem item) {
		return getEndpoint(item, false, "Upload");
	}

	/**
	 * Obtains a new endpoint for writing on the repository item.
	 *
	 * @param itemId
	 *            the id of an existing repository item
	 * @return the URL of the writing endpoint
	 */
	public String getWriteEndpoint(String itemId) {
		return getEndpoint(itemId, false, "Upload");
	}

	// action should be "Download" or "Upload"
	private String getEndpoint(RepositoryItem item, boolean toRead,
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
		log.info("Created {} for repo item #{}\n\ttimeout={}, url={}", type,
				item.getId(), endpoint.getAutoTerminationTimeout(),
				endpoint.getURL());

		endpoint.addSessionStartedListener(new RepositoryHttpEventListener<HttpSessionStartedEvent>() {
			@Override
			public void onEvent(HttpSessionStartedEvent event) {
				log.info("{} started on repo item #{}", action, event
						.getSource().getRepositoryItem().getId());
			}
		});

		endpoint.addSessionTerminatedListener(new RepositoryHttpEventListener<HttpSessionTerminatedEvent>() {
			@Override
			public void onEvent(HttpSessionTerminatedEvent event) {
				log.info("{} terminated on repo item #{}", action, event
						.getSource().getRepositoryItem().getId());
				// TODO should we do something about this??
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

	// action should be "Download" or "Upload"
	private String getEndpoint(String itemId, boolean toRead,
			final String action) {
		RepositoryItem item = repository.findRepositoryItemById(itemId);
		if (item == null)
			throw new KurentoException("No repository item found with id "
					+ itemId);
		return getEndpoint(item, toRead, action);
	}
}
