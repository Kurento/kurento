package org.kurento.client.internal.client;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.kurento.client.internal.ParamAnnotationUtils;
import org.kurento.jsonrpc.Props;

public class FactoryInvocationHandler extends DefaultInvocationHandler {

	private final Class<?> clazz;
	private final RemoteObjectFactory factory;

	public FactoryInvocationHandler(Class<?> clazz, RemoteObjectFactory factory) {
		super();
		this.clazz = clazz;
		this.factory = factory;
	}

	@Override
	public Object internalInvoke(Object proxy, Method method, Object[] args)
			throws Throwable {

		Props props = ParamAnnotationUtils.extractProps(
				method.getParameterAnnotations(), args);

		return Proxy.newProxyInstance(proxy.getClass().getClassLoader(),
				new Class[] { method.getReturnType() },
				new BuilderInvocationHandler(clazz, props, factory));
	}
}
