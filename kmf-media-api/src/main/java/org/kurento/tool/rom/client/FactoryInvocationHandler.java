package org.kurento.tool.rom.client;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.kurento.kmf.jsonrpcconnector.Props;
import org.kurento.tool.rom.ParamAnnotationUtils;

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
