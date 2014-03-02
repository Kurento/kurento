package com.kurento.tool.rom.client;

import java.lang.reflect.Type;

import com.kurento.kmf.jsonrpcconnector.Props;
import com.kurento.kmf.media.Continuation;
import com.kurento.tool.rom.server.RomException;

public abstract class RomClient {

	// Sync methods --------------------------------------

	public abstract String create(String remoteClassName,
			Props constructorParams) throws RomException;

	public abstract <E> E invoke(String objectRef, String methodName,
			Props params, Class<E> clazz) throws RomException;

	public abstract Object invoke(String objectRef, String operationName,
			Props operationParams, Type type) throws RomException;

	public abstract void release(String objectRef) throws RomException;

	public abstract String subscribe(String objectRef, String eventType);

	public abstract void keepAlive(String objectRef);

	// Async methods --------------------------------------

	public abstract String create(String remoteClassName,
			Props constructorParams, Continuation<String> cont)
			throws RomException;

	public abstract Object invoke(String objectRef, String operationName,
			Props operationParams, Type type, Continuation<?> cont);

	public abstract void release(String objectRef, Continuation<Void> cont)
			throws RomException;

	public abstract String subscribe(String objectRef, String type,
			Continuation<String> cont);

	public abstract void keepAlive(String objectRef,
			Continuation<Void> continuation);

	// Other methods --------------------------------------

	public abstract void addRomEventHandler(RomEventHandler eventHandler);

	public abstract void destroy();

}
