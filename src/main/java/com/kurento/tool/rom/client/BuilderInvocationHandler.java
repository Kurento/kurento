package com.kurento.tool.rom.client;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.kurento.kmf.jsonrpcconnector.Props;

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
			return Proxy.newProxyInstance(clazz.getClassLoader(),
					new Class[] { clazz }, new RemoteObjectInvocationHandler(
							remoteObject, factory));

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
