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

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.kurento.commons.exception.KurentoException;
import org.kurento.repository.service.pojo.RepositoryItemPlayer;
import org.kurento.repository.service.pojo.RepositoryItemRecorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for the REST API of Kurento Repository.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
@RestController
@RequestMapping(value = "/repo/item", produces = "application/json")
public class RepositoryController {

	@Autowired
	private RepositoryService repoService;

	@RequestMapping(method = RequestMethod.POST)
	public RepositoryItemRecorder createRepositoryItem(
			@RequestBody(required = false) Map<String, String> metadata) {
		return repoService.createRepositoryItem(metadata);
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{itemId}")
	public void removeRepositoryItem(@PathVariable("itemId") String itemId,
			HttpServletResponse response) {
		try {
			repoService.removeRepositoryItem(itemId);
		} catch (ItemNotFoundException e) {
			try {
				response.sendError(HttpStatus.NOT_FOUND.value(), e.getMessage());
			} catch (IOException ioe) {
				ioe.printStackTrace();
				throw new KurentoException(ioe);
			}
		}
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{itemId}")
	public RepositoryItemPlayer getReadEndpoint(
			@PathVariable("itemId") String itemId, HttpServletResponse response) {
		try {
			return repoService.getReadEndpoint(itemId);
		} catch (ItemNotFoundException e) {
			try {
				response.sendError(HttpStatus.NOT_FOUND.value(), e.getMessage());
			} catch (IOException ioe) {
				ioe.printStackTrace();
				throw new KurentoException(ioe);
			}
			return null;
		}
	}

	@RequestMapping(method = RequestMethod.POST, value = "/find")
	public Set<String> simpleFindItems(
			@RequestBody(required = true) Map<String, String> searchValues) {
		return repoService.findItems(searchValues, false);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/find/regex")
	public Set<String> regexFindItems(
			@RequestBody(required = true) Map<String, String> searchValues) {
		return repoService.findItems(searchValues, true);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{itemId}/metadata")
	public Map<String, String> getRepositoryItemMetadata(
			@PathVariable("itemId") String itemId, HttpServletResponse response) {
		try {
			return repoService.getRepositoryItemMetadata(itemId);
		} catch (ItemNotFoundException e) {
			try {
				response.sendError(HttpStatus.NOT_FOUND.value(), e.getMessage());
			} catch (IOException ioe) {
				ioe.printStackTrace();
				throw new KurentoException(ioe);
			}
			return null;
		}
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{itemId}/metadata")
	public void setRepositoryItemMetadata(
			@RequestBody(required = true) Map<String, String> metadata,
			@PathVariable("itemId") String itemId, HttpServletResponse response) {
		try {
			repoService.setRepositoryItemMetadata(itemId, metadata);
		} catch (ItemNotFoundException e) {
			try {
				response.sendError(HttpStatus.NOT_FOUND.value(), e.getMessage());
			} catch (IOException ioe) {
				ioe.printStackTrace();
				throw new KurentoException(ioe);
			}
		}
	}
}
