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

		private String transportId;
		private long pingInterval = -1;
		private volatile ScheduledFuture<?> lastTask;

		private long firstPingArrivalTime;
		private int currentPingMeasures = 0;
		private String sessionId;
		
		private Runnable closeSessionTask = new Runnable() {
			public void run() {
				log.info("Closing session with sessionId={} and transportId={} for not receiving ping in "
						+ (pingInterval * NUM_NO_PINGS_TO_CLOSE) + " millis", sessionId, transportId);
				closer.closeSession(transportId);
			}
		};

		public PingWatchdogSession(String transportId) {
			this.transportId = transportId;
		}

		public void pingReceived() {

			if (pingInterval == -1) {

				// First ping is ignored because its receiving time is not very
				// precise
				if (currentPingMeasures == 1) {
					firstPingArrivalTime = System.currentTimeMillis();
				} else if (currentPingMeasures == NUM_NO_PINGS_TO_CLOSE + 1) {
					pingInterval = (long) (((double) (System
							.currentTimeMillis() - firstPingArrivalTime)) / NUM_NO_PINGS_TO_CLOSE);

					log.info("Measured ping interval in " + pingInterval
							+ " millis");
					activateSessionCloser();
				}
				currentPingMeasures++;

			} else {
				activateSessionCloser();
			}
		}

		private void activateSessionCloser() {
			if (lastTask != null) {
				lastTask.cancel(false);
			}

			lastTask = taskScheduler.schedule(closeSessionTask, new Date(
					System.currentTimeMillis()
							+ (NUM_NO_PINGS_TO_CLOSE * pingInterval)));
		}

		public void setSessionId(String sessionId) {
			this.sessionId = sessionId;			
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

	public void associateSessionId(String transportId, String sessionId){
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

	private PingWatchdogSession getOrCreatePingSession(String transportId) {
		PingWatchdogSession session = sessions.get(transportId);
		if (session == null) {
			session = new PingWatchdogSession(transportId);
			sessions.put(transportId, session);
		}
		return session;
	}

	public void setPingWatchdog(boolean pingWachdog) {
		this.pingWachdog = pingWachdog;
	}

	public void removeSession(ServerSession session) {
		sessions.remove(session.getSessionId());
	}

}
