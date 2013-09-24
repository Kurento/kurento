package com.kurento.kmf.media.internal;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.common.exception.Assert;
import com.kurento.kmf.media.pool.MediaServerClientPoolService;
import com.kurento.kms.thrift.api.MediaObjectRef;
import com.kurento.kms.thrift.api.MediaServerService;
import com.kurento.kms.thrift.api.mediaServerConstants;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.keepAlive_call;

public class DistributedGarbageCollector {

	@Autowired
	protected MediaServerClientPoolService clientPool;

	private ConcurrentHashMap<MediaObjectRef, Integer> refCounters = new ConcurrentHashMap<MediaObjectRef, Integer>();
	private ConcurrentHashMap<MediaObjectRef, Timer> timers = new ConcurrentHashMap<MediaObjectRef, Timer>();

	public synchronized void registerReference(final MediaObjectRef objectRef) {
		Assert.notNull(objectRef, "", 30000); // TODO: message and error code

		Timer timer = null;

		Integer counter = refCounters.get(objectRef);
		if (counter == null) {
			counter = 1;
		} else {
			counter++;
		}
		refCounters.put(objectRef, counter);

		timer = new Timer(true);
		timers.put(objectRef, timer);

		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				keepAlive(objectRef);
			}
			// TODO: put the constant in upper case in the thrift interface
		}, mediaServerConstants.GarbagePeriod,
				mediaServerConstants.GarbagePeriod); // TODO: period
	}

	public void removeReference(MediaObjectRef objectRef) {
		Assert.notNull(objectRef, "", 30000); // TODO: message and error code
		Integer counter = refCounters.remove(objectRef);
		if (counter == null) {
			return;
		} else if (--counter > 0) {
			refCounters.put(objectRef, counter);
		} else {
			Timer timer = timers.remove(objectRef);
			if (timer == null) {
				//TODO: Log error, this should never happen
			}
			timer.cancel();
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
								public void onError(Exception exception) {
									clientPool.release(asyncClient);
									// TODO Auto-generated method stub
									// TODO: log error
								}

								@Override
								public void onComplete(keepAlive_call response) {
									clientPool.release(asyncClient);
								}
							});
		} catch (TException e) {
			// TODO Auto-generated catch block
			// TODO log error and propagate exception
			e.printStackTrace();
		}
	}
}
