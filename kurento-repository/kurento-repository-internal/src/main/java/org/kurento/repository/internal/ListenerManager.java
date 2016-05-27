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

package org.kurento.repository.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.repository.HttpSessionErrorEvent;
import org.kurento.repository.HttpSessionStartedEvent;
import org.kurento.repository.HttpSessionTerminatedEvent;
import org.kurento.repository.RepositoryHttpEventListener;
import org.kurento.repository.RepositoryHttpSessionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("rawtypes")
public class ListenerManager {

  private static final Logger log = LoggerFactory.getLogger(ListenerManager.class);

  private final Map<Class<? extends RepositoryHttpSessionEvent>, List<RepositoryHttpEventListener>> listeners =
      new ConcurrentHashMap<>();

  public void addStartedEventListener(
      RepositoryHttpEventListener<HttpSessionStartedEvent> listener) {
    addListener(listener, HttpSessionStartedEvent.class);
  }

  public void addTerminatedEventListener(
      RepositoryHttpEventListener<HttpSessionTerminatedEvent> listener) {
    addListener(listener, HttpSessionTerminatedEvent.class);
  }

  public void addErrorEventListener(RepositoryHttpEventListener<HttpSessionErrorEvent> listener) {
    addListener(listener, HttpSessionErrorEvent.class);
  }

  // TODO Improve concurrency
  protected synchronized <E extends RepositoryHttpSessionEvent> void addListener(
      RepositoryHttpEventListener<E> listener, Class<E> eventType) {

    List<RepositoryHttpEventListener> listenersType = listeners.get(eventType);

    if (listenersType == null) {
      listenersType = new ArrayList<>();
      listeners.put(eventType, listenersType);
    }

    listenersType.add(listener);
  }

  @SuppressWarnings("unchecked")
  public void fireEvent(RepositoryHttpSessionEvent event) {

    List<RepositoryHttpEventListener> listenersType = listeners.get(event.getClass());

    if (listenersType != null) {
      for (RepositoryHttpEventListener listener : listenersType) {
        try {
          listener.onEvent(event);
        } catch (Exception e) {
          log.warn("Exception while executing an event listener", e);
        }
      }
    }
  }

}
