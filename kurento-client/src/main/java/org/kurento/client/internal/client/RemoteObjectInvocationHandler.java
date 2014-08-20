package org.kurento.client.internal.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Arrays;

import org.kurento.client.Continuation;
import org.kurento.client.Event;
import org.kurento.client.EventListener;
import org.kurento.client.internal.ParamAnnotationUtils;
import org.kurento.client.internal.server.EventSubscription;
import org.kurento.client.internal.server.FactoryMethod;
import org.kurento.jsonrpc.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteObjectInvocationHandler extends DefaultInvocationHandler {

	private static final Logger LOG = LoggerFactory
			.getLogger(RemoteObjectInvocationHandler.class);

	private final RemoteObject remoteObject;
	private final RemoteObjectFactory factory;

	@SuppressWarnings("unchecked")
	public static <E> E newProxy(RemoteObject remoteObject,
			RemoteObjectFactory factory, Class<E> clazz) {

		RemoteObjectInvocationHandler handler = new RemoteObjectInvocationHandler(
				remoteObject, factory);
		Object proxy = Proxy.newProxyInstance(clazz.getClassLoader(),
				new Class[] { clazz }, handler);

		// This automatically unflatten this remote object with the wrapper
		// instead of with the remote object itself
		remoteObject.setWrapperForUnflatten(proxy);

		return (E) proxy;
	}

	private RemoteObjectInvocationHandler(RemoteObject remoteObject,
			RemoteObjectFactory factory) {
		this.remoteObject = remoteObject;
		this.factory = factory;
	}

	@Override
	public Object internalInvoke(final Object proxy, Method method,
			Object[] args) throws Throwable {

		Continuation<?> cont = null;
		if (args != null && args[args.length - 1] instanceof Continuation) {
			cont = (Continuation<?>) args[args.length - 1];
			args = Arrays.copyOf(args, args.length - 1);
		}

		String methodName = method.getName();

		if (method.getAnnotation(FactoryMethod.class) != null) {

			Props props = ParamAnnotationUtils.extractProps(
					method.getParameterAnnotations(), args);

			return createBuilderObject(proxy, method, methodName, props);

		} else if (methodName.equals("release")) {

			return release(cont);

		} else if (method.getAnnotation(EventSubscription.class) != null) {

			EventSubscription eventSubscription = method
					.getAnnotation(EventSubscription.class);

			return subscribeEventListener(proxy, args, methodName,
					eventSubscription.value(), cont);

		} else {

			return invoke(method, args, cont);
		}
	}

	private Object createBuilderObject(final Object proxy, Method method,
			String methodName, Props props) {

		if (props == null) {
			props = new Props();
		}

		FactoryMethod annotation = method.getAnnotation(FactoryMethod.class);
		props.add(annotation.value(), remoteObject.getObjectRef());

		Class<?> builderClass = method.getReturnType();

		return Proxy.newProxyInstance(this.getClass().getClassLoader(),
				new Class[] { method.getReturnType() },
				new BuilderInvocationHandler(builderClass.getEnclosingClass(),
						props, factory));

	}

	private Object invoke(Method method, Object[] args, Continuation<?> cont) {

		Props props = ParamAnnotationUtils.extractProps(
				method.getParameterAnnotations(), args);

		if (cont != null) {

			Type[] paramTypes = method.getGenericParameterTypes();
			ParameterizedType contType = (ParameterizedType) paramTypes[paramTypes.length - 1];
			Type returnType = contType.getActualTypeArguments()[0];
			remoteObject.invoke(method.getName(), props, returnType, cont);
			return null;
		}

		return remoteObject.invoke(method.getName(), props,
				method.getGenericReturnType());

	}

	@SuppressWarnings("unchecked")
	private Object release(Continuation<?> cont) {
		if (cont != null) {
			remoteObject.release((Continuation<Void>) cont);
		} else {
			remoteObject.release();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private Object subscribeEventListener(final Object proxy,
			final Object[] args, String methodName,
			final Class<? extends Event> eventClass, Continuation<?> cont) {

		String eventName = eventClass.getSimpleName().substring(0,
				eventClass.getSimpleName().length() - "Event".length());

		RemoteObject.RemoteObjectEventListener listener = new RemoteObject.RemoteObjectEventListener() {
			@Override
			public void onEvent(String eventType, Props data) {
				propagateEventTo(proxy, eventClass, data,
						(EventListener<?>) args[0]);
			}
		};

		if (cont != null) {
			remoteObject.addEventListener(eventName,
					(Continuation<ListenerSubscriptionImpl>) cont, listener);
			return null;
		} else {
			return remoteObject.addEventListener(eventName, listener);
		}
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

	public RemoteObject getRemoteObject() {
		return remoteObject;
	}

	public RemoteObjectFactory getFactory() {
		return factory;
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
		RemoteObjectInvocationHandler other = (RemoteObjectInvocationHandler) obj;
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
