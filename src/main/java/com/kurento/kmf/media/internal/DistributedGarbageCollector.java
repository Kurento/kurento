package com.kurento.kmf.media.internal;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.common.exception.Assert;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.pool.MediaServerClientPoolService;
import com.kurento.kms.thrift.api.MediaObjectRef;
import com.kurento.kms.thrift.api.MediaServerService;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.keepAlive_call;
import com.kurento.kms.thrift.api.mediaServerConstants;

public class DistributedGarbageCollector {

	private static final Logger log = LoggerFactory
			.getLogger(DistributedGarbageCollector.class);

	private static final long GARBAGE_PERIOD_MILIS = mediaServerConstants.GARBAGE_PERIOD * 1000;

	@Autowired
	protected MediaServerClientPoolService clientPool;

	private final ConcurrentHashMap<Long, Integer> refCounters = new ConcurrentHashMap<Long, Integer>();
	// TODO Let spring manage this timers with a TimerTaskExecutor
	private final ConcurrentHashMap<Long, Timer> timers = new ConcurrentHashMap<Long, Timer>();

	public synchronized void registerReference(final MediaObjectRef objectRef) {
		Assert.notNull(objectRef, "", 30000); // TODO: message and error code

		Timer timer = null;

		Integer counter = refCounters.get(objectRef);
		if (counter == null) {
			counter = 1;
		} else {
			counter++;
		}
		refCounters.put(Long.valueOf(objectRef.id), counter);

		// TODO Let spring manage this timers with a TimerTaskExecutor
		timer = new Timer(true);
		timers.put(Long.valueOf(objectRef.id), timer);

		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				keepAlive(objectRef);
			}
		}, GARBAGE_PERIOD_MILIS, GARBAGE_PERIOD_MILIS);
	}

	public void removeReference(MediaObjectRef objectRef) {
		Assert.notNull(objectRef, "", 30000); // TODO: message and error code
		Integer counter = refCounters.remove(objectRef);
		if (counter == null) {
			return;
		} else if (--counter > 0) {
			refCounters.put(Long.valueOf(objectRef.id), counter);
		} else {
			Timer timer = timers.remove(objectRef);
			if (timer == null) {
				log.error("Inconsistent state in DistributedGarbageCollector");
			} else {
				timer.cancel();
			}
		}
	}

	private void keepAlive(MediaObjectRef mediaObjectRef) {
		final MediaServerService.AsyncClient asyncClient = clientPool
				.acquireAsync();
		try {
			asyncClient
					.keepAlive(
							mediaObjectRef,
							new AsyncMethodCallback<MediaServerService.AsyncClient.keepAlive_call>() {

								@Override
								public void onError(Exception e) {
									clientPool.release(asyncClient);
									log.error(e.getMessage(), e);
								}

								@Override
								public void onComplete(keepAlive_call response) {
									clientPool.release(asyncClient);
								}
							});
		} catch (TException e) {
			log.error(e.getMessage(), e);
			// TODO message and error code
			throw new KurentoMediaFrameworkException(" ", e, 30000);
		}
	}
}
