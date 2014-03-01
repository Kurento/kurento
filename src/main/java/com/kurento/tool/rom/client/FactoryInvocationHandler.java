package com.kurento.tool.rom.client;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.kurento.kmf.jsonrpcconnector.Props;
import com.kurento.tool.rom.ParamAnnotationUtils;

public class FactoryInvocationHandler extends DefaultInvocationHandler {

	private Class<?> clazz;
	private RemoteObjectFactory factory;

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
