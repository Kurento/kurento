///*
// * (C) Copyright 2013 Kurento (http://kurento.org/)
// *
// * All rights reserved. This program and the accompanying materials
// * are made available under the terms of the GNU Lesser General Public License
// * (LGPL) version 2.1 which accompanies this distribution, and is available at
// * http://www.gnu.org/licenses/lgpl-2.1.html
// *
// * This library is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// * Lesser General Public License for more details.
// *
// */
//package com.kurento.kmf.connector;
//
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.util.HashMap;
//import java.util.Map;
//
//import org.apache.thrift.TBase;
//import org.apache.thrift.async.AsyncMethodCallback;
//import org.junit.Assert;
//import org.junit.Test;
//
//import com.google.gson.Gson;
//import com.kurento.kmf.connector.internal.AsyncCallbackFacadeProxy;
//import com.kurento.kmf.connector.internal.ConnectorServletImpl;
//import com.kurento.kmf.connector.internal.ServletTransaction;
//import com.kurento.kmf.connector.internal.protocol.JsonRpcRequest;
//import com.kurento.kms.thrift.api.KmsMediaServerService;
//
///**
// * @author Ivan Gracia (igracia@gsyc.es)
// * 
// */
//public class GsonTest {
//
//	/**
//	 * 
//	 */
//	private static final Map<String, Method> METHOD_MAP;
//
//	/**
//	 * 
//	 */
//	private final Transaction tx = new ServletTransaction(0L);
//
//	static {
//		Method[] methods = KmsMediaServerService.AsyncClient.class.getMethods();
//		METHOD_MAP = new HashMap<String, Method>(
//				(int) ((methods.length / 0.75) + 1));
//
//		for (Method method : methods) {
//			METHOD_MAP.put(method.getName(), method);
//		}
//	}
//
//	/**
//	 * Request.
//	 */
//	private final String jsonRequest = "{" + "\"jsonrpc\": \"2.0\","
//			+ "\"id\": 1," + "\"method\": \"subscribeEvent\"," + "\"params\":"
//			+ "{ \"mediaObjectRef\": {  \"id\": 1234,"
//			+ " \"token\": \"asdf\" }, \"eventType\": \"close\","
//			+ "\"handlerPort\": \"12\", \"handlerAddress\": \"192.168.0.1\"}"
//			+ "}";
//
//	// getThriftArgsFieldsClass(TBase args)
//	// getThriftMethodArgsClass(String method)
//	// getThriftMethodClass(String method)
//
//	@SuppressWarnings("rawtypes")
//	@Test
//	public void thriftReflection() throws NoSuchMethodException,
//			SecurityException, IllegalAccessException,
//			IllegalArgumentException, InvocationTargetException {
//		String methodName = "subscribeEvent";
//
//		Class argsClazz = KmsMediaServerService.subscribeEvent_args.class;
//		Class outArgsClazz = (Class) invokeSaticMethod(
//				ConnectorServletImpl.class, "getThriftMethodArgsClass",
//				new Class<?>[] { String.class }, new Object[] { methodName });
//		Assert.assertEquals(argsClazz, outArgsClazz);
//
//		Class fieldsClazz = KmsMediaServerService.subscribeEvent_args._Fields.class;
//		Class outfieldsClazz = (Class) invokeSaticMethod(
//				ConnectorServletImpl.class,
//				"getThriftArgsFieldsClass",
//				new Class<?>[] { TBase.class },
//				new Object[] { new KmsMediaServerService.subscribeEvent_args() });
//		Assert.assertEquals(fieldsClazz, outfieldsClazz);
//	}
//
//	private Object invokeSaticMethod(Class<?> targetClass, String methodName,
//			Class<?>[] argClasses, Object[] argObjects)
//			throws NoSuchMethodException, SecurityException,
//			IllegalAccessException, IllegalArgumentException,
//			InvocationTargetException {
//		Method method = targetClass.getDeclaredMethod(methodName, argClasses);
//		method.setAccessible(true);
//		return method.invoke(null, argObjects);
//	}
//
//	@SuppressWarnings("unchecked")
//	@Test
//	public void JsonDeserialisation() throws ClassNotFoundException,
//			SecurityException, IllegalArgumentException {
//		Gson gson = new Gson();
//		JsonRpcRequest req = gson.fromJson(jsonRequest, JsonRpcRequest.class);
//
//		String argsClassName = KmsMediaServerService.class.getName() + '$'
//				+ req.getMethod() + "_args";
//		Class<? extends TBase<?, ?>> argClass = (Class<? extends TBase<?, ?>>) Class
//				.forName(argsClassName);
//
//		Object temp = gson.fromJson(req.getParams(), argClass);
//
//		// String fieldClassName = argsClassName + "$_Fields";
//		// Class<? extends Enum> fieldsClass = (Class<? extends Enum>) Class
//		// .forName(fieldClassName);
//		//
//		// for (Entry<String, JsonElement> entry : req.getParams().entrySet()) {
//		// // Obtain the field value for each field
//		// Method m = fieldsClass.getMethod("findByName", String.class);
//		// TFieldIdEnum field = (TFieldIdEnum) m.invoke(null, entry.getKey());
//		//
//		// Field mapField = argClass.getDeclaredField("metaDataMap");
//		//
//		// Map metadataMap = new EnumMap(fieldsClass);
//		// metadataMap = (Map) mapField.get(metadataMap);
//		// FieldValueMetaData metadata = ((FieldMetaData) (metadataMap
//		// .get(field))).valueMetaData;
//		//
//		// Class<?> clazz = getThriftClass(metadata);
//		// Object kmsObj = gson.fromJson(entry.getValue(), clazz);
//		// Assert.assertNotNull(metadata);
//		// }
//		//
//		Assert.assertNotNull(temp);
//	}
//
//	@Test
//	public void callbackProxy() throws ClassNotFoundException,
//			IllegalAccessException, IllegalArgumentException,
//			InvocationTargetException {
//		Gson gson = new Gson();
//		JsonRpcRequest req = gson.fromJson(jsonRequest, JsonRpcRequest.class);
//
//		String callClassName = KmsMediaServerService.AsyncClient.class
//				.getName(); // + '$' + req.getMethod() + "_call";
//		Class callClass = Class.forName(callClassName);
//
//		AsyncMethodCallback callback = (AsyncMethodCallback) AsyncCallbackFacadeProxy
//				.newInstance(tx, callClass,
//						new Class[] { AsyncMethodCallback.class });
//
//		Assert.assertNotNull(callback);
//	}
//
// }
