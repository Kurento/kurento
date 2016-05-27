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

package org.kurento.repository;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public interface RepositoryItem {

  enum State {
    NEW, STORING, STORED
  }

  String getId();

  Map<String, String> getMetadata();

  void setMetadata(Map<String, String> metadata);

  void putMetadataEntry(String key, String value);

  /**
   * Returns the state of the RepositoryItem.
   *
   * @return NEW when the item has been just created and has no binary content yet. CONTENT when the
   *         item has binary content and can be read.
   */
  State getState();

  /**
   * Creates an InputStream to read for the contents of the item. This operation is only valid when
   * the item is in {@link RepositoryItem.State#STORED STORED} state.
   *
   * @return An input stream to read item content
   */
  InputStream createInputStreamToRead();

  /**
   * Creates an OutputStream to write the binary content of the file. This operation is only valid
   * when the item is in NEW state and change the item's state to
   * {@link RepositoryItem.State#STORING}. When the {@link OutputStream#close()} method is invoked,
   * the item's state is changed to {@link RepositoryItem.State#STORED STORED}. This method can be
   * called only once because only one {@link OutputStream} can be created.
   *
   * @return An output stream to write item content
   */
  OutputStream createOutputStreamToWrite();

  /**
   * Returns the {@link RepositoryHttpPlayer} to download the contents of the item using http
   * protocol. This operation is only valid when the item is in {@link RepositoryItem.State#STORED
   * STORED} state.
   *
   * @return A player to download the item via HTTP
   */
  RepositoryHttpPlayer createRepositoryHttpPlayer();

  /**
   * Returns the {@link RepositoryHttpPlayer} to download the contents of the item using http
   * protocol. This operation is only valid when the item is in {@link RepositoryItem.State#STORED
   * STORED} state. The parameter {@code
   * sessionIdInURL} allows to specify the sessionId of this player used to construct the URL. The
   * complete URL of the player can be obtained using the {@link RepositoryHttpPlayer#getURL()} in
   * the returned object.
   *
   * @param sessionIdInUrl
   *          The sessionId of this player used to construct the URL.
   * @return A player to download the item via HTTP
   */
  RepositoryHttpPlayer createRepositoryHttpPlayer(String sessionIdInUrl);

  /**
   * Returns the {@link RepositoryHttpRecorder} to upload the contents of the item using http
   * protocol. This operation is only valid when the item is in NEW state. When the element is used
   * using the provided URL, the state of the item is changed to {@link RepositoryItem.State#STORING
   * STORING}.
   *
   * @return A recorder to upload to the item via HTTP
   */
  RepositoryHttpRecorder createRepositoryHttpRecorder();

  /**
   * Returns the {@link RepositoryHttpRecorder} to upload the contents of the item using http
   * protocol. This operation is only valid when the item is in NEW state. When the element is used
   * using the provided URL, the state of the item is changed to {@link RepositoryItem.State#STORING
   * STORING}. The parameter {@code sessionIdInURL} allows to specify the sessionId of this recorder
   * used to construct the URL. The complete URL of the recorder can be obtained using the
   * {@link RepositoryHttpRecorder#getURL()} in the returned object.
   *
   * @param sessionIdInUrl
   *          The sessionId of this player used to construct the URL.
   * @return A recorder to upload to the item via HTTP
   */
  RepositoryHttpRecorder createRepositoryHttpRecorder(String sessionIdInUrl);

  /**
   * Returns the attributes associated with this {@link RepositoryItem}. This attributes are used
   * mainly when serving this item by means of http endpoint.
   *
   * @return The metainformation attributes of the item
   */
  RepositoryItemAttributes getAttributes();

}
