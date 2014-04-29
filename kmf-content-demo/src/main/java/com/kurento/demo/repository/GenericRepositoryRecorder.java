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
package com.kurento.demo.repository;

import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.HttpRecorderSession;
import com.kurento.kmf.repository.Repository;
import com.kurento.kmf.repository.RepositoryItem;

/**
 * Static class which contains a generic implementation of an HTTP Recorder.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.1
 */
public class GenericRepositoryRecorder {

	public static final Logger log = LoggerFactory
			.getLogger(GenericRepositoryRecorder.class);

	public static void record(HttpRecorderSession contentSession, String itemId) {
		Repository repository = contentSession.getRepository();
		RepositoryItem repositoryItem;
		try {
			repositoryItem = repository.findRepositoryItemById(itemId);
			log.info("Deleting existing repository '{}'", itemId);
			repository.remove(repositoryItem);
		} catch (NoSuchElementException e) {
			log.info("Repository item '{}' does not previously exist", itemId);
		}
		repositoryItem = contentSession.getRepository().createRepositoryItem(
				itemId);
		contentSession.start(repositoryItem);
	}
}
