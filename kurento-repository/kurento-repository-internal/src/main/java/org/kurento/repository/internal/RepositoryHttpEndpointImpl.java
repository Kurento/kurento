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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.io.output.ProxyOutputStream;
import org.kurento.commons.exception.KurentoException;
import org.kurento.repository.HttpSessionErrorEvent;
import org.kurento.repository.HttpSessionStartedEvent;
import org.kurento.repository.HttpSessionTerminatedEvent;
import org.kurento.repository.RepositoryHttpEndpoint;
import org.kurento.repository.RepositoryHttpEventListener;
import org.kurento.repository.RepositoryItem;
import org.kurento.repository.internal.http.RepositoryHttpManager;

public class RepositoryHttpEndpointImpl implements RepositoryHttpEndpoint {

  private final RepositoryHttpManager httpManager;
  private final RepositoryItem repositoryItem;

  private final String sessionId;
  private final String url;

  private OutputStream os;

  private final ListenerManager listeners = new ListenerManager();

  private long disconnectionTimeoutInMillis = 5000;

  @SuppressWarnings("rawtypes")
  private volatile ScheduledFuture lastStartedTimerFuture;

  private boolean startedEventFired;

  private long writtenBytes;
  private boolean outputStreamClosed;

  public RepositoryHttpEndpointImpl(RepositoryItem repositoryItem, String sessionId, String url,
      RepositoryHttpManager httpManager) {
    this.repositoryItem = repositoryItem;
    this.sessionId = sessionId;
    this.url = url;
    this.httpManager = httpManager;
  }

  @Override
  public String getURL() {
    return url;
  }

  @Override
  public String getDispatchURL() {
    return httpManager.getDispatchUrl(sessionId);
  }

  @Override
  public void setAutoTerminationTimeout(long timeoutInMillis) {
    this.disconnectionTimeoutInMillis = timeoutInMillis;
  }

  @Override
  public long getAutoTerminationTimeout() {
    return disconnectionTimeoutInMillis;
  }

  @Override
  public void addSessionStartedListener(
      RepositoryHttpEventListener<HttpSessionStartedEvent> listener) {
    listeners.addStartedEventListener(listener);
  }

  private void fireMediaSessionStartedEvent(HttpSessionStartedEvent event) {
    listeners.fireEvent(event);
  }

  @Override
  public void addSessionTerminatedListener(
      RepositoryHttpEventListener<HttpSessionTerminatedEvent> listener) {
    listeners.addTerminatedEventListener(listener);
  }

  private void fireMediaSessionTerminatedEvent(HttpSessionTerminatedEvent event) {
    listeners.fireEvent(event);
  }

  @Override
  public void addSessionErrorListener(RepositoryHttpEventListener<HttpSessionErrorEvent> listener) {
    listeners.addErrorEventListener(listener);
  }

  public synchronized void fireStartedEventIfFirstTime() {
    if (!startedEventFired) {
      fireMediaSessionStartedEvent(new HttpSessionStartedEvent(this));
      startedEventFired = true;
    }
  }

  public void fireSessionTerminatedEvent() {
    fireMediaSessionTerminatedEvent(new HttpSessionTerminatedEvent(this));
  }

  @Override
  public RepositoryItem getRepositoryItem() {
    return repositoryItem;
  }

  @Override
  public InputStream createRepoItemInputStream() {
    return repositoryItem.createInputStreamToRead();
  }

  @Override
  public OutputStream getRepoItemOutputStream() {

    if (outputStreamClosed) {
      throw new IllegalStateException("The outputStream is closed");
    }

    if (os == null) {
      os = new ProxyOutputStream(repositoryItem.createOutputStreamToWrite()) {

        @Override
        protected void afterWrite(int n) throws IOException {
          addWrittenBytes(n);
        }

        @Override
        public void close() throws IOException {
          super.close();
          outputStreamClosed = true;
        }
      };
    }
    return os;
  }

  private void addWrittenBytes(int numBytes) {
    writtenBytes += numBytes;
  }

  public long getWrittenBytes() {
    return writtenBytes;
  }

  // TODO Review for potentially race conditions if the timer is cancelled at
  // the same time it is executing
  public void stopInTimeout() {

    lastStartedTimerFuture = httpManager.getScheduler().schedule(new Runnable() {
      @Override
      public void run() {
        stop();
      }
    }, new Date(System.currentTimeMillis() + disconnectionTimeoutInMillis));
  }

  public void stopCurrentTimer() {
    if (lastStartedTimerFuture != null) {
      lastStartedTimerFuture.cancel(false);
    }
  }

  public String getSessionId() {
    return sessionId;
  }

  public void fireSessionErrorEvent(Exception ex) {
    listeners.fireEvent(new HttpSessionErrorEvent(this, ex));
  }

  public void forceStopHttpManager(String message) {
    stopTimerAndCloseOS();
    listeners.fireEvent(new HttpSessionErrorEvent(this, message));
  }

  // TODO Review for potentially race conditions if the timer is cancelled at
  // the same time it is executing
  // TODO Investigate how to "lock" the item when is been served to a client.
  // If we don't do, we can obtain a closed stream exception
  @Override
  public void stop() {

    httpManager.disposeHttpRepoItemElem(sessionId);
    stopTimerAndCloseOS();
    fireSessionTerminatedEvent();
  }

  private void stopTimerAndCloseOS() {
    if (lastStartedTimerFuture != null) {
      lastStartedTimerFuture.cancel(false);
      lastStartedTimerFuture = null;
    }

    if (os != null) {
      try {
        os.close();
      } catch (IOException e) {
        throw new KurentoException(e);
      }
    }
  }

}
