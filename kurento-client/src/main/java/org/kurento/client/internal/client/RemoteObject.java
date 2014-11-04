package org.kurento.client.internal.client;

import java.lang.reflect.Type;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.kurento.client.Continuation;
import org.kurento.client.KurentoObject;
import org.kurento.client.Transaction;
import org.kurento.client.TransactionNotExecutedException;
import org.kurento.client.internal.TransactionImpl;
import org.kurento.client.internal.client.operation.InvokeOperation;
import org.kurento.client.internal.client.operation.ReleaseOperation;
import org.kurento.client.internal.client.operation.SubscriptionOperation;
import org.kurento.client.internal.transport.serialization.ParamsFlattener;
import org.kurento.jsonrpc.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class RemoteObject {

	private static Logger LOG = LoggerFactory.getLogger(RemoteObject.class);

	private static ParamsFlattener FLATTENER = ParamsFlattener.getInstance();

	private String objectRef;
	private final String type;
	private boolean created;
	private final RomManager manager;

	private KurentoObject kurentoObject;

	private volatile CountDownLatch readyLatch;
	private Continuation<Object> whenContinuation;
	private Executor executor;

	private final Multimap<String, RemoteObjectEventListener> listeners = Multimaps
			.synchronizedMultimap(ArrayListMultimap
					.<String, RemoteObjectEventListener> create());

	public RemoteObject(String objectRef, String type, RomManager manager) {
		this(objectRef, type, true, manager);
	}

	public RemoteObject(String objectRef, String type, boolean created,
			RomManager manager) {
		this.objectRef = objectRef;
		this.manager = manager;
		this.type = type;
		this.created = created;

		this.manager.registerObject(objectRef, this);
	}

	public boolean isCommited() {
		return created;
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
				whenContinuation.onSuccess(this.getKurentoObject());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			executor.execute(new Runnable() {
				public void run() {
					try {
						whenContinuation.onSuccess(RemoteObject.this
								.getKurentoObject());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		}
	}

	public KurentoObject getKurentoObject() {
		return kurentoObject;
	}

	public void setKurentoObject(KurentoObject kurentoObject) {
		this.kurentoObject = kurentoObject;
	}

	public String getObjectRef() {
		return objectRef;
	}

	public String getType() {
		return type;
	}

	public RomManager getRomManager() {
		return manager;
	}

	@SuppressWarnings("unchecked")
	public <E> E invoke(String method, Props params, Class<E> clazz) {

		checkCreated();

		Type flattenType = FLATTENER.calculateFlattenType(clazz);

		Object obj = invoke(method, params, flattenType);

		return (E) FLATTENER.unflattenValue("return", clazz, obj, manager);
	}

	public Object invoke(String method, Props params, Type type) {

		checkCreated();

		Type flattenType = FLATTENER.calculateFlattenType(type);

		Object obj = manager.invoke(objectRef, method, params, flattenType);

		return FLATTENER.unflattenValue("return", type, obj, manager);
	}

	public Future<Object> invoke(String method, Props params, Type type,
			Transaction tx) {

		TransactionImpl txImpl = (TransactionImpl) tx;
		return txImpl.addOperation(new InvokeOperation(getKurentoObject(),
				method, params, type));
	}

	@SuppressWarnings("rawtypes")
	public void invoke(String method, Props params, final Type type,
			final Continuation cont) {

		checkCreated();

		Type flattenType = FLATTENER.calculateFlattenType(type);

		manager.invoke(objectRef, method, params, flattenType,
				new DefaultContinuation<Object>(cont) {
					@SuppressWarnings("unchecked")
					@Override
					public void onSuccess(Object result) {
						try {
							cont.onSuccess(FLATTENER.unflattenValue("return",
									type, result, manager));
						} catch (Exception e) {
							log.warn(
									"[Continuation] error invoking onSuccess implemented by client",
									e);
						}
					}
				});
	}

	public void release() {

		checkCreated();

		manager.release(objectRef);
	}

	public void release(Transaction tx) {
		TransactionImpl txImpl = (TransactionImpl) tx;
		txImpl.addOperation(new ReleaseOperation(getKurentoObject()));
	}

	public void release(final Continuation<Void> cont) {

		checkCreated();

		manager.release(objectRef, new DefaultContinuation<Void>(cont) {
			@Override
			public void onSuccess(Void result) {
				try {
					cont.onSuccess(null);
				} catch (Exception e) {
					log.warn(
							"[Continuation] error invoking onSuccess implemented by client",
							e);
				}
			}
		});
	}

	public ListenerSubscriptionImpl addEventListener(String eventType,
			RemoteObjectEventListener listener) {

		checkCreated();

		String subscription = manager.subscribe(objectRef, eventType);

		listeners.put(eventType, listener);

		return new ListenerSubscriptionImpl(subscription, eventType, listener);
	}

	public ListenerSubscriptionImpl addEventListener(String eventType,
			RemoteObjectEventListener listener, Transaction tx) {
		TransactionImpl txImpl = (TransactionImpl) tx;
		SubscriptionOperation op = new SubscriptionOperation(
				getKurentoObject(), eventType, listener);
		txImpl.addOperation(op);
		return op.getListenerSubscription();
	}

	public void addEventListener(final String eventType,
			final RemoteObjectEventListener listener,
			final Continuation<ListenerSubscriptionImpl> cont) {

		checkCreated();

		manager.subscribe(objectRef, eventType,
				new DefaultContinuation<String>(cont) {
					@Override
					public void onSuccess(String subscription) {
						listeners.put(eventType, listener);
						try {
							cont.onSuccess(new ListenerSubscriptionImpl(
									subscription, eventType, listener));
						} catch (Exception e) {
							log.warn(
									"[Continuation] error invoking onSuccess implemented by client",
									e);
						}
					}
				});
	}

	public void fireEvent(String type, Props data) {
		for (RemoteObjectEventListener eventListener : this.listeners.get(type)) {
			try {
				eventListener.onEvent(type, data);
			} catch (Exception e) {
				LOG.error("Exception executing event listener", e);
			}
		}
	}

	public Transaction beginTransaction() {
		return new TransactionImpl(manager);
	}

	private void checkCreated() {
		if (!created) {
			throw new TransactionNotExecutedException();
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((objectRef == null) ? 0 : objectRef.hashCode());
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
		RemoteObject other = (RemoteObject) obj;
		if (objectRef == null) {
			if (other.objectRef != null) {
				return false;
			}
		} else if (!objectRef.equals(other.objectRef)) {
			return false;
		}
		return true;
	}

	public void setCreatedObjectRef(String objectRef) {
		this.objectRef = objectRef;
		this.created = true;
		createReadyLatchIfNecessary();
		readyLatch.countDown();
		if (whenContinuation != null) {
			execWhenCommited();
		}
	}
}
