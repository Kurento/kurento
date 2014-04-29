package com.kurento.tool.rom.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public abstract class DefaultInvocationHandler implements InvocationHandler {

	private static final Set<String> DEFAULT_METHODS = ImmutableSet.of(
			"toString", "notify", "notifyAll", "wait", "getClass", "clone",
			"equals", "hashCode");

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {

		String methodName = method.getName();
		if (DEFAULT_METHODS.contains(methodName)) {
			return this.getClass().getMethod(methodName).invoke(this);
		} else {
			return internalInvoke(proxy, method, args);
		}
	}

	protected abstract Object internalInvoke(Object proxy, Method method,
			Object[] args) throws Throwable;

}
