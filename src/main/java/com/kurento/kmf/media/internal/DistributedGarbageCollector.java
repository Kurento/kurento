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
import com.kurento.kmf.media.internal.pool.MediaServerClientPoolService;
import com.kurento.kms.thrift.api.MediaObjectRef;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient;
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

		Long refId = Long.valueOf(objectRef.id);
		Integer counter = refCounters.get(refId);
		if (counter == null) {
			counter = Integer.valueOf(1);
		} else {
			counter++;
		}
		refCounters.put(refId, counter);

		// TODO Let spring manage this timers with a TimerTaskExecutor
		Timer timer = new Timer(true);
		timers.put(refId, timer);

		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				keepAlive(objectRef);
			}
		}, GARBAGE_PERIOD_MILIS, GARBAGE_PERIOD_MILIS);
	}

	public void removeReference(MediaObjectRef objectRef) {
		Assert.notNull(objectRef, "", 30000); // TODO: message and error code
		Long refId = Long.valueOf(objectRef.id);
		Integer counter = refCounters.remove(refId);

		if (counter == null) {
			return;
		} else if (--counter > 0) {
			refCounters.put(refId, counter);
		} else {
			Timer timer = timers.remove(refId);
			if (timer == null) {
				log.error("Inconsistent state in DistributedGarbageCollector");
			} else {
				timer.cancel();
			}
		}
	}

	private void keepAlive(MediaObjectRef mediaObjectRef) {
		final AsyncClient asyncClient = clientPool.acquireAsync();
		try {
			asyncClient.keepAlive(mediaObjectRef,
					new AsyncMethodCallback<keepAlive_call>() {

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
