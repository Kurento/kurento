package org.kurento.tool.rom.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.kurento.jsonrpcconnector.Props;
import org.kurento.media.Continuation;
import org.kurento.media.events.Event;
import org.kurento.media.events.MediaEventListener;
import org.kurento.tool.rom.ParamAnnotationUtils;
import org.kurento.tool.rom.server.FactoryMethod;

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

		} else if (methodName.startsWith("add")
				&& methodName.endsWith("Listener")) {

			return subscribeEventListener(proxy, args, methodName, cont);

		} else {

			return invoke(method, args, cont);
		}
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
			final Object[] args, String methodName, Continuation<?> cont) {

		String event = methodName.substring(3,
				methodName.length() - "Listener".length());

		RemoteObject.EventListener listener = new RemoteObject.EventListener() {
			@Override
			public void onEvent(String eventType, Props data) {
				propagateEventTo(proxy, eventType, data,
						(MediaEventListener<?>) args[0]);
			}
		};

		if (cont != null) {
			remoteObject.addEventListener(event,
					(Continuation<ListenerSubscription>) cont, listener);
			return null;
		}

		return remoteObject.addEventListener(event, listener);

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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void propagateEventTo(Object object, String eventType,
			Props data, MediaEventListener<?> listener) {

		// TODO Optimise this to create only one event for all listeners

		try {

			Class<?> eventClass = Class.forName("org.kurento.media.events."
					+ eventType + "Event");

			Constructor<?> constructor = eventClass.getConstructors()[0];

			Object[] params = ParamAnnotationUtils.extractEventParams(
					constructor.getParameterAnnotations(), data);

			params[0] = object;

			Event e = (Event) constructor.newInstance(params);

			((MediaEventListener) listener).onEvent(e);

		} catch (Exception e) {
			LOG.error("Exception while processing event '" + eventType
					+ "' with params '" + data + "'", e);
		}
	}

	public RemoteObject getRemoteObject() {
		return remoteObject;
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
