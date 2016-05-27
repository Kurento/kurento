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

package org.kurento.repository.internal.repoimpl;

import java.util.Map;

import org.kurento.repository.RepositoryHttpPlayer;
import org.kurento.repository.RepositoryHttpRecorder;
import org.kurento.repository.RepositoryItem;
import org.kurento.repository.RepositoryItemAttributes;

public abstract class AbstractRepositoryItem implements RepositoryItem {

  protected RepositoryWithHttp repository;
  protected String id;
  protected volatile State state;
  protected RepositoryItemAttributes attributes;
  protected Map<String, String> metadata;

  public AbstractRepositoryItem(String id, State state, RepositoryItemAttributes attributes,
      RepositoryWithHttp repository) {
    this.repository = repository;
    this.id = id;
    this.state = state;
    this.attributes = attributes;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public State getState() {
    return state;
  }

  protected void checkState(State desiredState) {
    if (state != desiredState) {
      throw new IllegalStateException("The item is in state \"" + state
          + "\" but is should be in state \"" + desiredState + "\"");
    }
  }

  @Override
  public RepositoryHttpPlayer createRepositoryHttpPlayer() {
    return repository.getRepositoryHttpManager().createRepositoryHttpPlayer(this);
  }

  @Override
  public RepositoryHttpPlayer createRepositoryHttpPlayer(String sessionIdInUrl) {
    return repository.getRepositoryHttpManager().createRepositoryHttpPlayer(this, sessionIdInUrl);
  }

  @Override
  public RepositoryHttpRecorder createRepositoryHttpRecorder() {
    return repository.getRepositoryHttpManager().createRepositoryHttpRecorder(this);
  }

  @Override
  public RepositoryHttpRecorder createRepositoryHttpRecorder(String sessionIdInUrl) {
    return repository.getRepositoryHttpManager().createRepositoryHttpRecorder(this, sessionIdInUrl);
  }

  @Override
  public RepositoryItemAttributes getAttributes() {
    return attributes;
  }

  @Override
  public Map<String, String> getMetadata() {
    return metadata;
  }

  @Override
  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  @Override
  public void putMetadataEntry(String key, String value) {
    this.metadata.put(key, value);
  }

}