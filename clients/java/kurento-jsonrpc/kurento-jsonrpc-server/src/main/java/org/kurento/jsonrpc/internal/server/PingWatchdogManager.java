/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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

package org.kurento.jsonrpc.internal.server;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;

public class PingWatchdogManager {

  private static final Logger log = LoggerFactory.getLogger(PingWatchdogManager.class);

  public interface NativeSessionCloser {
    public void closeSession(String transportId);
  }

  private static final long NUM_NO_PINGS_TO_CLOSE = 3;

  public class PingWatchdogSession {

    private static final long MAX_PING_INTERVAL = 20000;

    private String transportId;
    private String sessionId;

    private long pingInterval = -1;

    private volatile ScheduledFuture<?> lastTask;

    private Runnable closeSessionTask = new Runnable() {
      @Override
      public void run() {
        log.debug("Closing session with sessionId={} and transportId={} for not receiving ping in {}"
            + " millis", sessionId, transportId, pingInterval * NUM_NO_PINGS_TO_CLOSE);
        closer.closeSession(transportId);
      }
    };

    public PingWatchdogSession(String transportId) {
      this.transportId = transportId;
    }

    public void pingReceived(long interval) {

      if (pingInterval == -1) {

        if (interval == -1) {
          pingInterval = MAX_PING_INTERVAL;
          log.warn("Received first ping request without 'interval'");
        } else {
          pingInterval = interval;
        }

        log.debug(
            "Setting ping interval to {}" + " millis in session with transportId={}. "
                + "Connection is closed if a ping is not received in {}x{}={} millis",
            pingInterval, this.transportId, pingInterval, NUM_NO_PINGS_TO_CLOSE,
            NUM_NO_PINGS_TO_CLOSE * pingInterval);
      }

      activateSessionCloser();
    }

    private void activateSessionCloser() {

      disablePingWatchdog();

      lastTask = taskScheduler.schedule(closeSessionTask,
          new Date(System.currentTimeMillis() + NUM_NO_PINGS_TO_CLOSE * pingInterval));
    }

    public void setSessionId(String sessionId) {
      this.sessionId = sessionId;
    }

    public void setTransportId(String transportId) {
      this.transportId = transportId;
      disablePingWatchdog();

      if (pingWachdog) {
        if (pingInterval != -1) {
          log.debug("Setting new transportId={} for sessionId={}. "
              + "Restarting timer to consider disconnected client if pings are not received in {}"
              + " millis", transportId, sessionId, NUM_NO_PINGS_TO_CLOSE * pingInterval);
          activateSessionCloser();
        }
      }
    }

    public void disablePingWatchdog() {
      if (lastTask != null) {
        lastTask.cancel(false);
      }
    }
  }

  private ConcurrentHashMap<String, PingWatchdogSession> sessions = new ConcurrentHashMap<>();
  private boolean pingWachdog = false;
  private TaskScheduler taskScheduler;
  private NativeSessionCloser closer;

  public PingWatchdogManager(TaskScheduler taskScheduler, NativeSessionCloser closer) {
    this.taskScheduler = taskScheduler;
    this.closer = closer;
  }

  public void associateSessionId(String transportId, String sessionId) {
    if (pingWachdog) {
      PingWatchdogSession session = getOrCreatePingSession(transportId);
      session.setSessionId(sessionId);
    }
  }

  public void pingReceived(String transportId, long interval) {
    if (pingWachdog) {
      PingWatchdogSession session = getOrCreatePingSession(transportId);
      session.pingReceived(interval);
    }
  }

  // TODO Improve concurrency
  private synchronized PingWatchdogSession getOrCreatePingSession(String transportId) {
    PingWatchdogSession session = sessions.get(transportId);
    if (session == null) {
      log.debug("Created PingWatchdogSession for transportId {}", transportId);
      session = new PingWatchdogSession(transportId);
      sessions.put(transportId, session);
    }
    return session;
  }

  public void setPingWatchdog(boolean pingWachdog) {
    this.pingWachdog = pingWachdog;
  }

  public void removeSession(ServerSession session) {
    log.debug("Removed PingWatchdogSession for transportId {}", session.getTransportId());
    PingWatchdogSession pingSession = sessions.remove(session.getTransportId());
    if (pingSession != null) {
      pingSession.disablePingWatchdog();
    }
  }

  public synchronized void updateTransportId(String transportId, String oldTransportId) {
    PingWatchdogSession session = sessions.remove(oldTransportId);
    if (session != null) {
      log.debug("Updated with new transportId {} the session with old transportId {}", transportId,
          oldTransportId);
      session.setTransportId(transportId);
      sessions.put(transportId, session);
    } else {
      if (pingWachdog) {
        log.warn("Trying to update transport for nonexistent session with oldTransportId {}",
            oldTransportId);
      }
    }
  }

  public void disablePingWatchdogForSession(String transportId) {
    PingWatchdogSession session = sessions.get(transportId);
    if (session != null) {
      log.debug("Disabling PingWatchdog for session with transportId {}", transportId);
      session.disablePingWatchdog();
    } else {
      if (pingWachdog) {
        log.warn("Trying to disable PingWatchdog for nonexistent session with transportId {}",
            transportId);
      }
    }
  }

}
