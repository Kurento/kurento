package com.kurento.tool.rom.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.jsonrpcconnector.Props;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.events.Event;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.tool.rom.ParamAnnotationUtils;
import com.kurento.tool.rom.server.FactoryMethod;

public class RemoteObjectInvocationHandler extends DefaultInvocationHandler {

	private static final Logger LOG = LoggerFactory
			.getLogger(RemoteObjectInvocationHandler.class);

	private RemoteObject remoteObject;
	private RemoteObjectFactory factory;

	public RemoteObjectInvocationHandler(RemoteObject remoteObject,
			RemoteObjectFactory factory) {
		this.remoteObject = remoteObject;
		this.factory = factory;
	}

	@Override
	public Object internalInvoke(final Object proxy, Method method, Object[] args)
			throws Throwable {

		Continuation<?> cont = null;
		if(args != null && args[args.length-1] instanceof Continuation) {
			cont = (Continuation<?>) args[args.length-1];
			args = Arrays.copyOf(args, args.length-1);
		} 
		
		String methodName = method.getName();

		if (method.getAnnotation(FactoryMethod.class) != null) {

			Props props = ParamAnnotationUtils.extractProps(
					method.getParameterAnnotations(), args);
			
			return createBuilderObject(proxy, method, methodName, props);

		} else if (methodName.equals("release")) {

			//TODO Remove this comment when release is implemented in MediaServer. 
			//TODO Implement release async
			//remoteObject.release();
			return null;

		} else if (methodName.startsWith("add")
				&& methodName.endsWith("Listener")) {

			return subscribeEventListener(proxy, args, methodName, cont);

		} else {
			
			Props props = ParamAnnotationUtils.extractProps(
					method.getParameterAnnotations(), args);
			
			return remoteObject.invoke(method.getName(), props,
					method.getGenericReturnType(), cont);
		}
	}

	private Object subscribeEventListener(final Object proxy,
			final Object[] args, String methodName, Continuation<?> cont) {
		String event = methodName.substring(3,
				methodName.length() - "Listener".length());

		ListenerSubscription subscription = remoteObject.addEventListener(
				event, cont, new RemoteObject.EventListener() {
					@Override
					public void onEvent(String eventType, Props data) {
						propagateEventTo(proxy, eventType, data,
								(MediaEventListener<?>) args[0]);
					}
				});

		return subscription;
	}

	private Object createBuilderObject(final Object proxy, Method method,
			String methodName, Props props) throws ClassNotFoundException {

		if(props == null) {
			props = new Props();
		}

		// TODO Make this paramName generic based on model. Now is coupled to
		// "mediaPipeline"
		props.add("mediaPipeline", remoteObject.getObjectRef());

		Class<?> builderClass = method.getReturnType();

		return Proxy.newProxyInstance(this.getClass().getClassLoader(),
				new Class[] { method.getReturnType() },
				new BuilderInvocationHandler(builderClass.getEnclosingClass(), props, factory));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void propagateEventTo(Object object, String eventType,
			Props data, MediaEventListener<?> listener) {

		//TODO Optimize this to create only one event for all listeners
		
		try {

			Class<?> eventClass = Class.forName("com.kurento.kmf.media.events."
					+ eventType + "Event");

			Constructor<?> constructor = eventClass.getConstructors()[0];

			Object[] params = ParamAnnotationUtils.extractEventParams(
					constructor.getParameterAnnotations(), data);

			params[0] = object;

			Event e = (Event) constructor.newInstance(params);

			((MediaEventListener) listener).onEvent(e);

		} catch (Exception e) {
			LOG.error("Exception propagating event '" + eventType
					+ "' with params " + data, e);
		}
	}

	public RemoteObject getRemoteObject() {
		return remoteObject;
	}

	@Override
	public String toString() {
		return "[RemoteObject: type="+this.remoteObject.getType()+" remoteRef="+remoteObject.getObjectRef()+"";
	}
}
