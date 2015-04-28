package org.kurento.jsonrpc.internal.server;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;

public class PingWatchdogManager {

	private static final Logger log = LoggerFactory
			.getLogger(PingWatchdogManager.class);

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
				log.info(
						"Closing session with sessionId={} and transportId={} for not receiving ping in {}"
								+ " millis", sessionId, transportId,
								pingInterval * NUM_NO_PINGS_TO_CLOSE);
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

				log.info(
						"Setting ping interval to {}"
								+ " millis in session with transportId={}. "
								+ "Connection is closed if a ping is not received in {}x{}={} millis",
								pingInterval, this.transportId, pingInterval,
								NUM_NO_PINGS_TO_CLOSE, NUM_NO_PINGS_TO_CLOSE
								* pingInterval);
			}

			activateSessionCloser();
		}

		private void activateSessionCloser() {

			disablePrevPingWatchdog();

			lastTask = taskScheduler.schedule(closeSessionTask,
					new Date(System.currentTimeMillis() + NUM_NO_PINGS_TO_CLOSE
							* pingInterval));
		}

		public void setSessionId(String sessionId) {
			this.sessionId = sessionId;
		}

		public void setTransportId(String transportId) {
			this.transportId = transportId;
			disablePrevPingWatchdog();

			if (pingWachdog) {
				if (pingInterval != -1) {
					log.info(
							"Setting new transportId={} for sessionId={}. "
									+ "Restarting timer to consider disconnected client if pings are not received in {}"
									+ " millis", transportId, sessionId,
									NUM_NO_PINGS_TO_CLOSE * pingInterval);
					activateSessionCloser();
				}
			}
		}

		private void disablePrevPingWatchdog() {
			if (lastTask != null) {
				lastTask.cancel(false);
			}
		}
	}

	private ConcurrentHashMap<String, PingWatchdogSession> sessions = new ConcurrentHashMap<>();
	private boolean pingWachdog = false;
	private TaskScheduler taskScheduler;
	private NativeSessionCloser closer;

	public PingWatchdogManager(TaskScheduler taskScheduler,
			NativeSessionCloser closer) {
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
	private synchronized PingWatchdogSession getOrCreatePingSession(
			String transportId) {
		PingWatchdogSession session = sessions.get(transportId);
		if (session == null) {
			log.info("Created PingWatchdogSession for transportId {}",
					transportId);
			session = new PingWatchdogSession(transportId);
			sessions.put(transportId, session);
		}
		return session;
	}

	public void setPingWatchdog(boolean pingWachdog) {
		this.pingWachdog = pingWachdog;
	}

	public void removeSession(ServerSession session) {
		log.info("Removed PingWatchdogSession for transportId {}",
				session.getTransportId());
		sessions.remove(session.getTransportId());
	}

	public synchronized void updateTransportId(String transportId,
			String oldTransportId) {
		PingWatchdogSession session = sessions.remove(oldTransportId);
		if (session != null) {
			log.info(
					"Updated with new transportId {} the session with old transportId {}",
					transportId, oldTransportId);
			session.setTransportId(transportId);
			sessions.put(transportId, session);
		} else {
			log.warn(
					"Trying to update transport for unexisting session with oldTransportId {}",
					oldTransportId);
		}
	}

	public void disablePingWatchdogForSession(String transportId) {
		PingWatchdogSession session = sessions.get(transportId);
		if (session != null) {
			log.info("Disabling PingWatchdog for session with transportId {}",
					transportId);
			session.disablePrevPingWatchdog();
		} else {
			log.warn(
					"Trying to disable PingWatchdog for unexisting session with transportId {}",
					transportId);
		}
	}

}
