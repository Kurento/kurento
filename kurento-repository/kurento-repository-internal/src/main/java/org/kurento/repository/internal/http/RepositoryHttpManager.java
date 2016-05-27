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

package org.kurento.repository.internal.http;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.kurento.commons.SecretGenerator;
import org.kurento.repository.RepositoryApiConfiguration;
import org.kurento.repository.RepositoryHttpEndpoint;
import org.kurento.repository.RepositoryHttpPlayer;
import org.kurento.repository.RepositoryHttpRecorder;
import org.kurento.repository.RepositoryItem;
import org.kurento.repository.internal.RepositoryHttpEndpointImpl;
import org.kurento.repository.internal.RepositoryHttpPlayerImpl;
import org.kurento.repository.internal.RepositoryHttpRecorderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class RepositoryHttpManager {

  @Autowired
  private RepositoryApiConfiguration config;

  private String webappPublicUrl;

  private String servletPath;

  private final ConcurrentMap<String, RepositoryHttpEndpointImpl> sessions =
      new ConcurrentHashMap<>();

  private final SecretGenerator generator = new SecretGenerator();

  @Autowired
  @Qualifier("repositoryTaskScheduler")
  private TaskScheduler scheduler;

  public RepositoryHttpPlayer createRepositoryHttpPlayer(RepositoryItem repositoryItem) {
    return (RepositoryHttpPlayer) createRepositoryHttpElem(repositoryItem,
        RepositoryHttpPlayer.class, null);
  }

  public RepositoryHttpPlayer createRepositoryHttpPlayer(RepositoryItem repositoryItem,
      String sessionIdInUrl) {
    return (RepositoryHttpPlayer) createRepositoryHttpElem(repositoryItem,
        RepositoryHttpPlayer.class, sessionIdInUrl);
  }

  public RepositoryHttpRecorder createRepositoryHttpRecorder(RepositoryItem repositoryItem) {
    return (RepositoryHttpRecorder) createRepositoryHttpElem(repositoryItem,
        RepositoryHttpRecorder.class, null);
  }

  public RepositoryHttpRecorder createRepositoryHttpRecorder(RepositoryItem repositoryItem,
      String sessionIdInUrl) {
    return (RepositoryHttpRecorder) createRepositoryHttpElem(repositoryItem,
        RepositoryHttpRecorder.class, sessionIdInUrl);
  }

  private RepositoryHttpEndpointImpl createRepositoryHttpElem(RepositoryItem repositoryItem,
      Class<? extends RepositoryHttpEndpoint> repoItemHttpElemClass, String sessionIdInUrl) {

    if (sessionIdInUrl == null) {
      sessionIdInUrl = createUniqueId();
    }

    String url = createUrl(sessionIdInUrl);

    RepositoryHttpEndpointImpl elem = null;

    if (repoItemHttpElemClass == RepositoryHttpPlayer.class) {
      elem = new RepositoryHttpPlayerImpl(repositoryItem, sessionIdInUrl, url, this);
    } else {
      elem = new RepositoryHttpRecorderImpl(repositoryItem, sessionIdInUrl, url, this);
    }

    sessions.put(sessionIdInUrl, elem);

    return elem;
  }

  private String createUniqueId() {
    return generator.nextSecret();
  }

  private String createUrl(String sessionId) {
    return webappPublicUrl + getDispatchUrl(sessionId);
  }

  public String getDispatchUrl(String id) {
    return servletPath + id;
  }

  public RepositoryHttpEndpointImpl getHttpRepoItemElem(String sessionId) {
    return sessionId == null ? null : sessions.get(sessionId);
  }

  public TaskScheduler getScheduler() {
    return scheduler;
  }

  public void disposeHttpRepoItemElem(String sessionId) {
    sessions.remove(sessionId);
  }

  public void disposeHttpRepoItemElemByItemId(RepositoryItem item, String message) {

    // We don't use another map indexed by RepositoryItemIds for several
    // reasons:
    // * Memory consumption
    // * More complex code (development time, difficult to maintain and
    // test)
    // * It is very unlike this operation is called in a reasonable use case

    Iterator<Entry<String, RepositoryHttpEndpointImpl>> it = sessions.entrySet().iterator();

    while (it.hasNext()) {
      Entry<String, RepositoryHttpEndpointImpl> entry = it.next();
      RepositoryHttpEndpointImpl elem = entry.getValue();
      if (elem.getRepositoryItem().getId().equals(item.getId())) {
        elem.forceStopHttpManager(message);
        it.remove();
      }
    }
  }

  public void setWebappPublicUrl(String webappUrl) {
    this.webappPublicUrl = webappUrl;
  }

  public void setServletPath(String servletPath) {
    this.servletPath = servletPath;
  }

}
