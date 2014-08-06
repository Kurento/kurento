package org.kurento.client.internal.client;

import java.lang.reflect.Method;

import org.kurento.client.Continuation;
import org.kurento.jsonrpc.Props;

public class BuilderInvocationHandler extends DefaultInvocationHandler {

	private final Props props;
	private final RemoteObjectFactory factory;
	private final Class<?> clazz;

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
							try {
								cont.onSuccess(RemoteObjectInvocationHandler
										.newProxy(remoteObject, factory, clazz));
							} catch (Exception e) {
								log.warn(
										"[Continuation] error invoking onSuccess implemented by client",
										e);
							}
						}
					});

			return null;

		} else {

			if (name.startsWith("with")) {

				String propName = extractAndLower("with", name);

				props.add(propName, args[0]);

			} else if (name.startsWith("not")) {

				String propName = extractAndLower("not", name);
				props.add(propName, Boolean.FALSE);

			} else {
				props.add(name, Boolean.TRUE);
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
