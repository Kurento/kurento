package org.kurento.tool.rom.client;

import java.lang.reflect.Proxy;

public class RemoteObjectTypedFactory {

	private RemoteObjectFactory factory;

	public RemoteObjectTypedFactory(RemoteObjectFactory factory) {
		this.factory = factory;
	}

	public <E> E create(Class<E> clazz) {
		RemoteObject remoteObject = factory.create(clazz.getSimpleName());
		return RemoteObjectInvocationHandler.newProxy(remoteObject, factory,
				clazz);
	}

	@SuppressWarnings("unchecked")
	public <F> F getFactory(Class<F> clazz) {
		return (F) Proxy
				.newProxyInstance(clazz.getClassLoader(),
						new Class[] { clazz }, new FactoryInvocationHandler(
								clazz.getEnclosingClass(), factory));
	}

	public void destroy() {
		factory.destroy();
	}

}
