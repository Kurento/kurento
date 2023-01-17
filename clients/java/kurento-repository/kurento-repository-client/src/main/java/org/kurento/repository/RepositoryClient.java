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
 */

package org.kurento.repository;

import java.util.Map;
import java.util.Set;

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
 * Client API for the Kurento repository server application. It uses REST as means of communicating
 * with the server.
 *
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public interface RepositoryClient {

  /**
   * Creates a new repository item with the provided metadata, ready for media recording.
   *
   * @param metadata
   *          a map of values. Can be empty but <strong>not null</strong>.
   * @return a {@link RepositoryItemRecorder} containing the item's id and an URL through which
   *         Kurento Media Server can record media sent by the client
   */
  @POST("/repo/item")
  RepositoryItemRecorder createRepositoryItem(@Body Map<String, String> metadata);

  /**
   * Removes the repository item associated to the provided id.
   *
   * @param itemId
   *          the id of an existing repository item
   * @return a {@link Response} containing the response code from the server
   */
  @DELETE("/repo/item/{itemId}")
  Response removeRepositoryItem(@Path("itemId") String itemId);

  /**
   * Obtains a new endpoint for reading (playing multimedia) from the repository item.
   *
   * @param itemId
   *          the id of an existing repository item
   * @return a {@link RepositoryItemPlayer} containing the item's id and an URL from which Kurento
   *         Media Server can play (serve) stored media streams
   */
  @GET("/repo/item/{itemId}")
  RepositoryItemPlayer getReadEndpoint(@Path("itemId") String itemId);

  /**
   * Searches for repository items by each pair of attributes and their expected values.
   *
   * @param searchValues
   *          pairs of attributes and their values
   * @return a {@link Set}&lt;{@link String}&gt; with identifiers of the repository items that were
   *         found
   */
  @POST("/repo/item/find")
  Set<String> simpleFindItems(@Body Map<String, String> searchValues);

  /**
   * Searches for repository items by each pair of attributes whose values can be regex Strings.
   *
   * @param searchValues
   *          pairs of attributes and their values
   * @return a {@link Set}&lt;{@link String}&gt; with identifiers of the repository items that were
   *         found
   */
  @POST("/repo/item/find/regex")
  Set<String> regexFindItems(@Body Map<String, String> searchValues);

  /**
   * Returns the metadata from a repository item.
   *
   * @param itemId
   *          the id of an existing repository item
   * @return the metadata map
   */
  @GET("/repo/item/{itemId}/metadata")
  Map<String, String> getRepositoryItemMetadata(@Path("itemId") String itemId);

  /**
   * Replaces the metadata of a repository item.
   *
   * @param itemId
   *          the id of an existing repository item
   * @param metadata
   *          the new metadata
   * @return a {@link Response} with the response code from the server
   */
  @PUT("/repo/item/{itemId}/metadata")
  Response setRepositoryItemMetadata(@Path("itemId") String itemId,
      @Body Map<String, String> metadata);

}
