package org.kurento.client.internal.client;

import java.lang.reflect.Type;
import java.util.List;

import org.kurento.client.Continuation;
import org.kurento.client.KurentoObject;
import org.kurento.client.TransactionNotExecutedException;
import org.kurento.client.internal.transport.serialization.ParamsFlattener;
import org.kurento.jsonrpc.Prop;
import org.kurento.jsonrpc.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class RemoteObject implements RemoteObjectFacade {

	private static Logger LOG = LoggerFactory.getLogger(RemoteObject.class);

	private static ParamsFlattener FLATTENER = ParamsFlattener.getInstance();

	public interface RemoteObjectEventListener {
		public void onEvent(String eventType, Props data);
	}

	private final RomManager manager;

	private final String objectRef;
	private final String type;

	// This object is used in the process of unflatten. It is common that
	// RemoteObject is used with a Typed wrapper (with reflexion, with code
	// generation or by hand). In this cases, the object reference is unflatten
	// to this value instead of RemoteObject itself.
	private KurentoObject publicObject;

	private final Multimap<String, RemoteObjectEventListener> listeners = Multimaps
			.synchronizedMultimap(ArrayListMultimap
					.<String, RemoteObjectEventListener> create());

	public RemoteObject(String objectRef, String type, RomManager manager) {
		this.manager = manager;

		this.objectRef = objectRef;
		this.type = type;

		this.manager.registerObject(objectRef, this);
	}

	@Override
	public KurentoObject getPublicObject() {
		return publicObject;
	}

	@Override
	public void setPublicObject(KurentoObject wrapperForUnflatten) {
		this.publicObject = wrapperForUnflatten;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> E invoke(String method, Props params, Class<E> clazz) {
		return (E) invoke(method, params, (Type) clazz);
	}

	@Override
	public Object invoke(String method, Props params, Type type) {

		Type flattenType = FLATTENER.calculateFlattenType(type);

		checkForNonReadyObjects(params);

		Object obj = manager.invoke(objectRef, method, params, flattenType);

		return FLATTENER.unflattenValue("return", type, obj, manager);
	}

	// TODO Can we optimize this without processing always all invoke params?
	private void checkForNonReadyObjects(Props params) {
		if (params != null) {
			for (Prop prop : params) {
				checkParam(prop.getValue());
			}
		}
	}

	private void checkParam(Object param) {
		if (param instanceof KurentoObject) {
			checkMediaObject((KurentoObject) param);
		} else if (param instanceof List) {
			for (Object elem : ((List<?>) param)) {
				checkParam(elem);
			}
		} else if (param instanceof Props) {
			for (Prop prop : ((Props) param)) {
				checkParam(prop.getValue());
			}
		}
	}

	private void checkMediaObject(KurentoObject mediaObject) {
		if (!mediaObject.isCommited()) {
			throw new TransactionNotExecutedException();
		}
	}

	@Override
	public void release() {
		manager.release(objectRef);
	}

	@Override
	public ListenerSubscriptionImpl addEventListener(String eventType,
			RemoteObjectEventListener listener) {

		String subscription = manager.subscribe(objectRef, eventType);

		addListener(eventType, listener);

		return new ListenerSubscriptionImpl(subscription, eventType, listener);
	}

	public void addListener(String eventType, RemoteObjectEventListener listener) {
		listeners.put(eventType, listener);
	}

	@Override
	public void addEventListener(final String eventType,
			final RemoteObjectEventListener listener,
			final Continuation<ListenerSubscriptionImpl> cont) {

		manager.subscribe(objectRef, eventType,
				new DefaultContinuation<String>(cont) {
					@Override
					public void onSuccess(String subscription) {
						addListener(eventType, listener);
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

	@Override
	public String getObjectRef() {
		return objectRef;
	}

	@Override
	public void fireEvent(String type, Props data) {
		for (RemoteObjectEventListener eventListener : this.listeners.get(type)) {
			try {
				eventListener.onEvent(type, data);
			} catch (Exception e) {
				LOG.error("Exception executing event listener", e);
			}
		}
	}

	@Override
	public String getType() {
		return type;
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

	@Override
	public RomManager getRomManager() {
		return manager;
	}

	@Override
	public void release(Continuation<Void> continuation) {
		manager.release(objectRef, continuation);
	}

}
