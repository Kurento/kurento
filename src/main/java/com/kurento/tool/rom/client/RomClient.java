package com.kurento.tool.rom.client;

import java.lang.reflect.Type;

import com.kurento.kmf.jsonrpcconnector.Props;
import com.kurento.kmf.media.Continuation;
import com.kurento.tool.rom.server.RomException;

public abstract class RomClient {

	public abstract String create(String remoteClassName,
			Props constructorParams) throws RomException;

	public abstract String create(String remoteClassName,
			Props constructorParams, Continuation<?> cont) throws RomException;

	public abstract <E> E invoke(String objectRef, String methodName,
			Props params, Class<E> clazz) throws RomException;

	public abstract <E> E invoke(String objectRef, String operationName,
			Props operationParams, Type type, Continuation<?> cont);

	public abstract <E> E invoke(String objectRef, String operationName,
			Props operationParams, Type type) throws RomException;

	public abstract void release(String objectRef) throws RomException;

	public abstract void release(String objectRef, Continuation<?> cont)
			throws RomException;

	public abstract void addRomEventHandler(RomEventHandler eventHandler);

	public abstract String subscribe(String objectRef, String eventType);

	public abstract String subscribe(String objectRef, String type,
			Continuation<?> cont);

	public abstract void destroy();

}
