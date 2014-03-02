package com.kurento.tool.rom.client;

import java.lang.reflect.Method;

import com.kurento.kmf.jsonrpcconnector.Props;
import com.kurento.kmf.media.Continuation;

public class BuilderInvocationHandler extends DefaultInvocationHandler {

	private Props props;
	private RemoteObjectFactory factory;
	private Class<?> clazz;

	public BuilderInvocationHandler(Class<?> clazz, Props props,
			RemoteObjectFactory factory) {
		this.clazz = clazz;
		this.props = props;
		this.factory = factory;
	}

	@Override
	public Object internalInvoke(Object proxy, Method method, Object[] args)
			throws Throwable {

		String name = method.getName();
		if (name.equals("build")) {

			RemoteObject remoteObject = factory.create(clazz.getSimpleName(),
					props);

			return RemoteObjectInvocationHandler.newProxy(remoteObject,
					factory, clazz);

		} else if (name.equals("buildAsync")) {

			@SuppressWarnings("rawtypes")
			final Continuation cont = (Continuation) args[args.length - 1];

			factory.create(clazz.getSimpleName(), props,
					new DefaultContinuation<RemoteObject>(cont) {
						@SuppressWarnings("unchecked")
						@Override
						public void onSuccess(RemoteObject remoteObject) {
							cont.onSuccess(RemoteObjectInvocationHandler
									.newProxy(remoteObject, factory, clazz));
						}
					});

			return null;

		} else {

			if (name.startsWith("with")) {

				String propName = extractAndLower("with", name);

				props.add(propName, args[0]);

			} else if (name.startsWith("not")) {

				String propName = extractAndLower("not", name);
				props.add(propName, false);

			} else {
				props.add(name, true);
			}

			return proxy;
		}
	}

	private String extractAndLower(String prefix, String name) {
		String propName = name.substring(prefix.length());
		return Character.toLowerCase(propName.charAt(0))
				+ propName.substring(1);
	}

}
