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

package org.kurento.repository.rest;

import java.util.Map;
import java.util.Set;

import org.kurento.repository.RepositoryController;
import org.kurento.repository.service.pojo.RepositoryItemPlayer;
import org.kurento.repository.service.pojo.RepositoryItemRecorder;

import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

/**
 * Client REST API for the Kurento repository server application.
 * 
 * @see RepositoryController
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public interface RepositoryRestApi {

	/**
	 * @param metadata
	 *            a map of values. Can be empty but <strong>not null</strong>.
	 * @return
	 */
	@POST("/repo/item")
	RepositoryItemRecorder createRepositoryItem(
			@Body Map<String, String> metadata);

	@DELETE("/repo/item/{itemId}")
	Response removeRepositoryItem(
			@Path("itemId") String itemId);

	@GET("/repo/item/{itemId}")
	RepositoryItemPlayer getReadEndpoint(@Path("itemId") String itemId);

	@POST("/repo/item/find")
	Set<String> simpleFindItems(@Body Map<String, String> searchValues);

	@POST("/repo/item/find/regex")
	Set<String> regexFindItems(@Body Map<String, String> searchValues);

	@GET("/repo/item/{itemId}/metadata")
	Map<String, String> getRepositoryItemMetadata(
			@Path("itemId") String itemId);

	@PUT("/repo/item/{itemId}/metadata")
	Response setRepositoryItemMetadata(@Path("itemId") String itemId,
			@Body Map<String, String> metadata);

}