package org.kurento.client;

import java.lang.reflect.Constructor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import org.kurento.client.internal.ParamAnnotationUtils;
import org.kurento.client.internal.TransactionImpl;
import org.kurento.client.internal.client.RemoteObject;
import org.kurento.client.internal.client.RemoteObjectFacade;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.client.operation.ReleaseOperation;
import org.kurento.jsonrpc.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KurentoObject {

	private static final Logger LOG = LoggerFactory
			.getLogger(KurentoObject.class);

	private MediaPipeline internalMediaPipeline;
	protected RemoteObjectFacade remoteObject;
	private volatile CountDownLatch readyLatch;
	private Continuation<Object> whenContinuation;

	protected TransactionImpl tx;

	private Executor executor;

	protected KurentoObject(RemoteObjectFacade remoteObject, Transaction tx) {
		setRemoteObject(remoteObject);
		this.tx = (TransactionImpl) tx;
	}

	void setInternalMediaPipeline(MediaPipeline internalMediaPipeline) {
		this.internalMediaPipeline = internalMediaPipeline;
	}

	MediaPipeline getInternalMediaPipeline() {
		return internalMediaPipeline;
	}

	public boolean isCommited() {
		return remoteObject instanceof RemoteObject;
	}

	public void waitCommited() throws InterruptedException {
		createReadyLatchIfNecessary();
		readyLatch.await();
	}

	private void createReadyLatchIfNecessary() {
		if (readyLatch == null) {
			synchronized (this) {
				if (readyLatch == null) {
					readyLatch = new CountDownLatch(1);
				}
			}
		}
	}

	public synchronized void whenCommited(Continuation<?> continuation) {
		whenCommited(continuation, null);
	}

	@SuppressWarnings("unchecked")
	public synchronized void whenCommited(Continuation<?> continuation,
			Executor executor) {
		this.whenContinuation = (Continuation<Object>) continuation;
		this.executor = executor;
		if (isCommited()) {
			execWhenCommited();
		}
	}

	private void execWhenCommited() {
		if (executor == null) {
			// TODO Propagate error if object is not ready for error
			try {
				whenContinuation.onSuccess(this);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			executor.execute(new Runnable() {
				public void run() {
					try {
						whenContinuation.onSuccess(this);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		}
	}

	synchronized void setRemoteObject(RemoteObjectFacade remoteObject) {
		this.remoteObject = remoteObject;
		this.remoteObject.setPublicObject(this);

		if (remoteObject instanceof RemoteObject) {
			tx = null;
			createReadyLatchIfNecessary();
			readyLatch.countDown();
			if (whenContinuation != null) {
				// TODO Propagate error if object is not ready for error
				execWhenCommited();
			}
		}
	}

	protected ListenerSubscription subscribeEventListener(
			final EventListener<?> clientListener,
			final Class<? extends Event> eventClass) {

		String eventName = eventClass.getSimpleName().substring(0,
				eventClass.getSimpleName().length() - "Event".length());

		RemoteObject.RemoteObjectEventListener listener = new RemoteObject.RemoteObjectEventListener() {
			@Override
			public void onEvent(String eventType, Props data) {
				propagateEventTo(KurentoObject.this, eventClass, data,
						clientListener);
			}
		};

		return remoteObject.addEventListener(eventName, listener);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void propagateEventTo(Object object,
			Class<? extends Event> eventClass, Props data,
			EventListener<?> listener) {

		// TODO Optimize this to create only one event for all listeners

		try {

			Constructor<?> constructor = eventClass.getConstructors()[0];

			Object[] params = ParamAnnotationUtils.extractEventParams(
					constructor.getParameterAnnotations(), data);

			params[0] = object;

			Event e = (Event) constructor.newInstance(params);

			((EventListener) listener).onEvent(e);

		} catch (Exception e) {
			LOG.error(
					"Exception while processing event '"
							+ eventClass.getSimpleName() + "' with params '"
							+ data + "'", e);
		}
	}

	RemoteObjectFacade getRemoteObject() {
		return remoteObject;
	}

	RomManager getRomManager() {
		return remoteObject.getRomManager();
	}

	@Override
	public String toString() {
		return "[MediaObject: type=" + this.remoteObject.getType()
				+ " remoteRef=" + remoteObject.getObjectRef() + "";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((remoteObject == null) ? 0 : remoteObject.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		KurentoObject other = (KurentoObject) obj;
		if (remoteObject == null) {
			if (other.remoteObject != null) {
				return false;
			}
		} else if (!remoteObject.equals(other.remoteObject)) {
			return false;
		}
		return true;
	}

	/**
	 *
	 * Explicitly release a media object form memory. All of its children will
	 * also be released.
	 *
	 **/
	public void release() {
		remoteObject.release();
	}

	public void release(Continuation<Void> continuation) {
		remoteObject.release(continuation);

	}

	public void release(Transaction tx) {
		((TransactionImpl) tx).addOperation(new ReleaseOperation(this));
	}

	public Transaction getCreationTransaction() {
		return tx;
	}
}
