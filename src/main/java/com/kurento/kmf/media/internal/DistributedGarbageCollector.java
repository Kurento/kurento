/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.kmf.media.internal;

import static com.kurento.kms.thrift.api.KmsMediaErrorCodesConstants.MEDIA_OBJECT_NOT_FOUND;
import static com.kurento.kms.thrift.api.KmsMediaServerConstants.DEFAULT_GARBAGE_COLLECTOR_PERIOD;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.common.exception.Assert;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.MediaObject;
import com.kurento.kmf.media.internal.pool.MediaServerClientPoolService;
import com.kurento.kms.thrift.api.KmsMediaObjectRef;
import com.kurento.kms.thrift.api.KmsMediaServerException;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.keepAlive_call;

public class DistributedGarbageCollector {

	private static final Logger log = LoggerFactory
			.getLogger(DistributedGarbageCollector.class);

	@Autowired
	protected MediaServerClientPoolService clientPool;

	@Autowired
	private MediaServerCallbackHandler handler;

	private final ConcurrentMap<Long, AtomicInteger> refCounters = new ConcurrentHashMap<Long, AtomicInteger>();
	// TODO Let spring manage this timers with a TimerTaskExecutor
	private final ConcurrentMap<Long, Timer> timers = new ConcurrentHashMap<Long, Timer>();

	/**
	 * Registers a {@link MediaObject} in the distributed garbage collector. A
	 * keepalive will be sent to the media server every. No reference to the
	 * objectRef is stored.
	 * 
	 * @param objectRef
	 */
	public void registerReference(final KmsMediaObjectRef objectRef) {
		registerReference(objectRef, DEFAULT_GARBAGE_COLLECTOR_PERIOD);
	}

	/**
	 * Registers a {@link MediaObject} in the distributed garbage collector. A
	 * keepalive will be sent to the media server every {@code collectorPeriod}
	 * seconds. No reference to the objectRef is stored.
	 * 
	 * @param objectRef
	 * @param collectorPeriod
	 */
	public void registerReference(final KmsMediaObjectRef objectRef,
			int collectorPeriod) {
		Assert.notNull(objectRef,
				"Invalid reference passed to DistributedGarbageCollector",
				30000); // TODO: message and error code

		Long refId = Long.valueOf(objectRef.id);

		AtomicInteger counter = refCounters.putIfAbsent(refId,
				new AtomicInteger(1));

		if (counter != null) {
			counter.incrementAndGet();
		}

		// TODO Let spring manage this timers with a TimerTaskExecutor
		Timer timer = new Timer(true);
		timers.put(refId, timer);

		long collectorPeriodInMilis = collectorPeriod * 1000;
		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				keepAlive(objectRef);
			}
		}, collectorPeriodInMilis, collectorPeriodInMilis);
	}

	/**
	 * Removes a reference to a {@link MediaObject} from the Distributed Garbage
	 * Collector. This implies that no more keepalives will be sent to the media
	 * server.
	 * 
	 * @param objectRef
	 * @return true if the object was removed. False if no reference to the
	 *         object was found.
	 */
	public boolean removeReference(final KmsMediaObjectRef objectRef) {
		Assert.notNull(objectRef, "", 30000); // TODO: message and error code
		Long refId = Long.valueOf(objectRef.id);

		AtomicInteger counter = refCounters.get(refId);
		if (counter == null) {
			return false;
		} else if (counter.decrementAndGet() > 0) {
			return true;
		}
		log.trace("Removing DGC reference for object: {}",
				String.valueOf(objectRef.id));
		refCounters.remove(refId);

		handler.removeAllListeners(refId);
		handler.removeAllErrorListeners(refId);

		Timer timer = timers.remove(refId);
		if (timer == null) {
			log.error("Inconsistent state in DistributedGarbageCollector: no timer found for a media object that was not collected.");
		} else {
			timer.cancel();
		}

		return true;
	}

	private void keepAlive(final KmsMediaObjectRef KmsMediaObjectRef) {
		final AsyncClient asyncClient = clientPool.acquireAsync();
		try {
			asyncClient.keepAlive(KmsMediaObjectRef,
					new AsyncMethodCallback<keepAlive_call>() {

						@Override
						public void onError(Exception e) {
							clientPool.release(asyncClient);
							log.error(e.getMessage(), e);
						}

						@Override
						public void onComplete(keepAlive_call response) {
							try {
								response.getResult();
							} catch (KmsMediaServerException e) {
								if (e.getErrorCode() == MEDIA_OBJECT_NOT_FOUND) {
									DistributedGarbageCollector.this
											.removeReference(KmsMediaObjectRef);
								}
								log.error(e.getMessage(), e);
							} catch (TException e) {
								log.error(e.getMessage(), e);
							} finally {
								clientPool.release(asyncClient);
							}
						}
					});
		} catch (TException e) {
			log.error(e.getMessage(), e);
			// TODO message and error code
			throw new KurentoMediaFrameworkException("Error sending keepalive",
					e, 30000);
		}
	}

}
