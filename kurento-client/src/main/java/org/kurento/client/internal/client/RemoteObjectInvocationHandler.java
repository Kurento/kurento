package org.kurento.client.internal.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.kurento.client.Continuation;
import org.kurento.client.Event;
import org.kurento.client.EventListener;
import org.kurento.client.KurentoObject;
import org.kurento.client.Transaction;
import org.kurento.client.internal.ParamAnnotationUtils;
import org.kurento.client.internal.server.EventSubscription;
import org.kurento.client.internal.transport.serialization.ParamsFlattener;
import org.kurento.jsonrpc.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

public class RemoteObjectInvocationHandler extends DefaultInvocationHandler {

	private static final Logger LOG = LoggerFactory
			.getLogger(RemoteObjectInvocationHandler.class);

	private static final Set<String> REMOTE_OBJECT_METHODS = ImmutableSet.of(
			"isCommited", "waitCommited", "whenCommited", "beginTransaction");

	private RemoteObject remoteObject;
	private final RomManager manager;

	@SuppressWarnings("unchecked")
	public static <E> E newProxy(RemoteObject remoteObject, RomManager manager,
			Class<E> clazz) {

		RemoteObjectInvocationHandler handler = new RemoteObjectInvocationHandler(
				remoteObject, manager);

		KurentoObject kurentoObject = (KurentoObject) Proxy.newProxyInstance(
				clazz.getClassLoader(), new Class[] { clazz }, handler);

		remoteObject.setKurentoObject(kurentoObject);

		return (E) kurentoObject;
	}

	public static RemoteObjectInvocationHandler getFor(Object object) {
		return (RemoteObjectInvocationHandler) Proxy
				.getInvocationHandler(object);
	}

	private RemoteObjectInvocationHandler(RemoteObject remoteObject,
			RomManager manager) {
		this.remoteObject = remoteObject;
		this.manager = manager;
	}

	@Override
	public Object internalInvoke(final Object proxy, Method method,
			Object[] args) throws Throwable {

		String methodName = method.getName();
		if (REMOTE_OBJECT_METHODS.contains(methodName)) {
			Method remoteObjectMethod = findMethod(remoteObject, methodName,
					args);
			return remoteObjectMethod.invoke(remoteObject, args);
		}

		LOG.debug("Invoking method {} on object {}", method, proxy);

		Continuation<?> cont = null;
		Transaction tx = null;
		List<String> paramNames = Collections.emptyList();

		if ((args != null) && (args.length > 0)) {

			paramNames = ParamAnnotationUtils.getParamNames(method);

			if (args[args.length - 1] instanceof Continuation) {

				cont = (Continuation<?>) args[args.length - 1];
				args = Arrays.copyOf(args, args.length - 1);
				paramNames = paramNames.subList(0, paramNames.size() - 1);

			} else if ((args != null) && (args.length > 0)
					&& (args[0] instanceof Transaction)) {

				tx = (Transaction) args[0];
				args = Arrays.copyOfRange(args, 1, args.length);
				paramNames = paramNames.subList(1, paramNames.size());
			}
		}

		if (methodName.equals("release")) {

			return release(cont, tx);

		} else if (method.getAnnotation(EventSubscription.class) != null) {

			EventSubscription eventSubscription = method
					.getAnnotation(EventSubscription.class);

			if (methodName.startsWith("add")) {
				return subscribeEventListener(proxy, args, methodName,
						eventSubscription.value(), cont, tx);
			} else if (methodName.startsWith("remove")) {
				return unsubscribeEventListener(proxy, args, methodName,
						eventSubscription.value(), cont, tx);
			} else {
				throw new IllegalStateException("Method " + methodName
						+ " undefined for events");
			}

		} else {

			return invoke(method, paramNames, args, cont, tx);
		}
	}

	private Object invoke(Method method, List<String> paramNames,
			Object[] args, Continuation<?> cont, Transaction tx) {

		Props props = ParamAnnotationUtils.extractProps(paramNames, args);

		if (cont != null) {

			Type[] paramTypes = method.getGenericParameterTypes();
			ParameterizedType contType = (ParameterizedType) paramTypes[paramTypes.length - 1];
			Type returnType = contType.getActualTypeArguments()[0];
			remoteObject.invoke(method.getName(), props, returnType, cont);
			return null;

		} else if (tx != null) {

			Type returnType = method.getGenericReturnType();

			if (returnType instanceof ParameterizedType) {
				ParameterizedType futureType = (ParameterizedType) returnType;
				Type methodReturnType = futureType.getActualTypeArguments()[0];
				return remoteObject.invoke(method.getName(), props,
						methodReturnType, tx);
			} else {
				return remoteObject.invoke(method.getName(), props, Void.class,
						tx);
			}

		} else {

			return remoteObject.invoke(method.getName(), props,
					method.getGenericReturnType());
		}
	}

	@SuppressWarnings("unchecked")
	private Object release(Continuation<?> cont, Transaction tx) {
		if (cont != null) {
			remoteObject.release((Continuation<Void>) cont);
		} else if (tx != null) {
			remoteObject.release(tx);
		} else {
			remoteObject.release();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private Object subscribeEventListener(final Object proxy,
			final Object[] args, String methodName,
			final Class<? extends Event> eventClass, Continuation<?> cont,
			Transaction tx) {

		String eventName = eventClass.getSimpleName().substring(0,
				eventClass.getSimpleName().length() - "Event".length());

		RemoteObjectEventListener listener = new RemoteObjectEventListener() {
			@Override
			public void onEvent(String eventType, Props data) {
				propagateEventTo(proxy, eventClass, data,
						(EventListener<?>) args[0]);
			}
		};

		if (cont != null) {
			remoteObject.addEventListener(eventName, listener,
					(Continuation<ListenerSubscriptionImpl>) cont);
			return null;
		} else if (tx != null) {
			return remoteObject.addEventListener(eventName, listener, tx);
		} else {
			return remoteObject.addEventListener(eventName, listener);
		}
	}

	@SuppressWarnings("unchecked")
	private Object unsubscribeEventListener(final Object proxy,
			final Object[] args, String methodName,
			final Class<? extends Event> eventClass, Continuation<?> cont,
			Transaction tx) {

		ListenerSubscriptionImpl listenerSubscription = (ListenerSubscriptionImpl) args[0];
		if (cont != null) {
			remoteObject.removeEventListener(listenerSubscription,
					(Continuation<Void>) cont);
		} else if (tx != null) {
			remoteObject.removeEventListener(listenerSubscription, tx);
		} else {
			remoteObject.removeEventListener(listenerSubscription);
		}

		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void propagateEventTo(Object object,
			Class<? extends Event> eventClass, Props data,
			EventListener<?> listener) {

		// TODO Optimize this to create only one event for all listeners

		try {

			Constructor<?> constructor = eventClass.getConstructors()[0];

			data.add("source", ((KurentoObject) object).getId());

			Object[] params = ParamsFlattener.getInstance().unflattenParams(
					constructor.getParameterAnnotations(),
					constructor.getGenericParameterTypes(), data, manager);

			Event e = (Event) constructor.newInstance(params);

			((EventListener) listener).onEvent(e);

		} catch (Exception e) {
			LOG.error(
					"Exception while processing event '"
							+ eventClass.getSimpleName() + "' with params '"
							+ data + "'", e);
		}
	}

	public RemoteObject getRemoteObject() {
		return remoteObject;
	}

	public void setRemoteObject(RemoteObject remoteObject) {
		this.remoteObject = remoteObject;
	}

	public RomManager getRomManager() {
		return manager;
	}

	@Override
	public String toString() {
		return "[RemoteObject: type=" + this.remoteObject.getType()
				+ " remoteRef=" + remoteObject.getObjectRef() + "";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result)
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
		RemoteObjectInvocationHandler other = getFor(obj);
		if (other == null) {
			return false;
		}
		if (remoteObject == null) {
			if (other.remoteObject != null) {
				return false;
			}
		} else if (!remoteObject.equals(other.remoteObject)) {
			return false;
		}
		return true;
	}

}
