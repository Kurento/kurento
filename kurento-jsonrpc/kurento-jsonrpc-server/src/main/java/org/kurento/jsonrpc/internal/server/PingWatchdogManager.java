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
		private long pingInterval = MAX_PING_INTERVAL;
		private boolean pingIntervalCalculated = false;
		private volatile ScheduledFuture<?> lastTask;

		private long firstPingArrivalTime;
		private int currentPingMeasures = 0;
		private String sessionId;

		private Runnable closeSessionTask = new Runnable() {
			public void run() {
				log.info(
						"Closing session with sessionId={} and transportId={} for not receiving ping in "
								+ (pingInterval * NUM_NO_PINGS_TO_CLOSE)
								+ " millis", sessionId, transportId);
				closer.closeSession(transportId);
			}
		};

		public PingWatchdogSession(String transportId) {
			this.transportId = transportId;
		}

		public void pingReceived() {

			if (!pingIntervalCalculated) {

				// First ping is ignored because its receiving time is not very
				// precise
				if (currentPingMeasures == 1) {
					firstPingArrivalTime = System.currentTimeMillis();
				} else if (currentPingMeasures == NUM_NO_PINGS_TO_CLOSE + 1) {
					pingInterval = (long) (((double) (System
							.currentTimeMillis() - firstPingArrivalTime)) / NUM_NO_PINGS_TO_CLOSE);

					pingIntervalCalculated = true;
					
					log.info("Measured ping interval in {}"
							+ " millis in session {} with transportId {}",
							pingInterval, sessionId, transportId);
					activateSessionCloser();
				}
				currentPingMeasures++;

			}
			
			activateSessionCloser();
		}

		private void activateSessionCloser() {
			
			disablePrevPingWatchdog();

			lastTask = taskScheduler.schedule(closeSessionTask,
					new Date(System.currentTimeMillis()
							+ (NUM_NO_PINGS_TO_CLOSE * pingInterval)));
		}

		public void setSessionId(String sessionId) {
			this.sessionId = sessionId;
		}

		public void setTransportId(String transportId) {
			this.transportId = transportId;
			disablePrevPingWatchdog();

			if (pingWachdog) {
				log.info("Restarting timer to consider disconnected client if pings are not received in "
						+ NUM_NO_PINGS_TO_CLOSE * pingInterval + " millis");
				activateSessionCloser();
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

	public void pingReceived(String transportId) {
		if (pingWachdog) {
			PingWatchdogSession session = getOrCreatePingSession(transportId);
			session.pingReceived();
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
			log.info(
					"Disabling PingWatchdog for session with transportId {}",
					transportId);
			session.disablePrevPingWatchdog();
		} else {
			log.warn(
					"Trying to disable PingWatchdog for unexisting session with transportId {}",
					transportId);
		}		
	}

}
