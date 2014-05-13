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
package com.kurento.tool.rom.client;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.common.exception.Assert;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcErrorException;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.MediaObject;

public class DistributedGarbageCollector {

	private static final Logger log = LoggerFactory
			.getLogger(DistributedGarbageCollector.class);

	private static final int DEFAULT_GARBAGE_COLLECTOR_PERIOD = 5;

	private final ConcurrentMap<String, AtomicInteger> refCounters = new ConcurrentHashMap<>();

	// TODO Let spring manage this timers with a TimerTaskExecutor
	private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();

	private final RomClient client;

	public DistributedGarbageCollector(RomClient client) {
		this.client = client;
	}

	/**
	 * Registers a {@link MediaObject} in the distributed garbage collector. A
	 * keepalive will be sent to the media server every
	 * DEFAULT_GARBAGE_COLLECTOR_PERIOD.
	 * 
	 * @param objectRef
	 */
	public void registerReference(final String objectRef) {
		registerReference(objectRef, DEFAULT_GARBAGE_COLLECTOR_PERIOD);
	}

	/**
	 * Registers a {@link MediaObject} in the distributed garbage collector. A
	 * keepalive will be sent to the media server every {@code collectorPeriod}
	 * seconds.
	 * 
	 * @param objectRef
	 * @param collectorPeriod
	 */
	public void registerReference(final String objectRef, int collectorPeriod) {

		Assert.notNull(
				objectRef,
				"An object reference must be provided in order to register an object in the DGC");

		AtomicInteger counter = refCounters.putIfAbsent(objectRef,
				new AtomicInteger(1));

		if (counter != null) {
			counter.incrementAndGet();
		}

		// TODO Let spring manage this timers with a TimerTaskExecutor
		Timer timer = new Timer(true);
		timers.put(objectRef, timer);

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
	public boolean removeReference(final String objectRef) {

		Assert.notNull(
				objectRef,
				"An object reference must be provided in order to remove an object from the DGC");

		AtomicInteger counter = refCounters.get(objectRef);
		if (counter == null) {
			return false;
		} else if (counter.decrementAndGet() > 0) {
			return true;
		}
		log.trace("Removing DGC reference for object: {}", objectRef);
		refCounters.remove(objectRef);

		Timer timer = timers.remove(objectRef);
		if (timer == null) {
			log.error(
					"Inconsistent state in DistributedGarbageCollector: "
							+ "no timer found for media object {} that was not collected.",
					objectRef);
		} else {
			timer.cancel();
		}

		return true;
	}

	private void keepAlive(final String objectRef) {

		client.keepAlive(objectRef, new Continuation<Void>() {
			@Override
			public void onError(Throwable e) {

				if (e instanceof JsonRpcErrorException) {
					int errorCode = ((JsonRpcErrorException) e).getCode();
					// TODO this should be obtained form a common place
					if (errorCode == -32000) {
						log.debug(
								"Keepalive sent, but object {} was not found in KMS. Will remove reference",
								objectRef);
						removeReference(objectRef);
					}
				}

				log.error(e.getMessage(), e);
			}

			@Override
			public void onSuccess(Void response) {
				log.trace("Keepalive sent for object: {}", objectRef);
			}
		});
	}

}