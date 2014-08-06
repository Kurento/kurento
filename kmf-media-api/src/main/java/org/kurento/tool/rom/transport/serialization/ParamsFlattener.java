package org.kurento.tool.rom.transport.serialization;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kurento.kmf.jsonrpcconnector.Prop;
import org.kurento.kmf.jsonrpcconnector.Props;
import org.kurento.tool.rom.ParamAnnotationUtils;
import org.kurento.tool.rom.RemoteClass;
import org.kurento.tool.rom.client.RemoteObject;
import org.kurento.tool.rom.client.RemoteObjectInvocationHandler;
import org.kurento.tool.rom.client.RomClientObjectManager;
import org.kurento.tool.rom.server.ProtocolException;
import org.kurento.tool.rom.server.RemoteObjectManager;

public class ParamsFlattener {

	private static final Logger log = LoggerFactory
			.getLogger(ParamsFlattener.class);

	public enum RomType {
		VOID, INTEGER, BOOLEAN, FLOAT, STRING, CT_ENUM, CT_REGISTER, LIST, REMOTE_CLASS
	}

	public static class GenericListType implements ParameterizedType {

		private final Type[] elementsTypes;

		public GenericListType(Type elementsType) {
			this.elementsTypes = new Type[] { elementsType };
		}

		@Override
		public Type[] getActualTypeArguments() {
			return elementsTypes;
		}

		@Override
		public Type getRawType() {
			return List.class;
		}

		@Override
		public Type getOwnerType() {
			return null;
		}

	}

	private static final ParamsFlattener INSTANCE = new ParamsFlattener();

	public static ParamsFlattener getInstance() {
		return INSTANCE;
	}

	/**
	 * Flatten the parameter list to be sent to remote server using flattenParam
	 * method
	 * 
	 * @param params
	 * @return Properties holding flattened params
	 */
	public Props flattenParams(Props params) {

		Props properties = new Props();
		for (Prop prop : params) {
			properties.add(prop.getName(), flattenParam(prop.getValue()));
		}
		return properties;
	}

	/**
	 * Flatten the parameter list to be sent to remote server using flattenParam
	 * method
	 * 
	 * @param params
	 * @return
	 */
	private List<?> flattenParamsList(List<? extends Object> params) {
		List<Object> plainParams = new ArrayList<>(params.size());
		for (Object param : params) {
			plainParams.add(flattenParam(param));
		}
		return plainParams;
	}

	/**
	 * Flatten param to be sent to remote server. The rules to flatten objects
	 * are:
	 * <ul>
	 * <li>If param is primitive (String, Boolean, Float or Integer) is not
	 * modified</li>
	 * <li>If param is enum value, is transformed to its String representation</li>
	 * <li>If param is an RemoteObject, is sent is reference String</li>
	 * <li>If param is a complex object, a Props object is created for it. The
	 * Props object has an entry for each property with its name and value. The
	 * value of the property is also flatten.</li>
	 * </ul>
	 * 
	 * @param param
	 * @return
	 */
	private Object flattenParam(Object param) {

		if (param == null) {
			return null;
		}

		Object processedParam;
		if (param instanceof RemoteObject) {
			processedParam = ((RemoteObject) param).getObjectRef();
		} else if (param instanceof Proxy) {

			InvocationHandler handler = Proxy.getInvocationHandler(param);
			if (handler instanceof RemoteObjectInvocationHandler) {
				RemoteObjectInvocationHandler roHandler = (RemoteObjectInvocationHandler) handler;
				processedParam = roHandler.getRemoteObject().getObjectRef();
			} else {
				throw new ProtocolException(
						"Only proxies from remote objects are allowed, but found one with InvocationHandler "
								+ handler);
			}

		} else if (param instanceof Enum<?>) {
			processedParam = param.toString();
		} else if (isPrimitive(param)) {
			processedParam = param;
		} else if (param instanceof List<?>) {
			processedParam = flattenParamsList((List<?>) param);
		} else if (param instanceof Props) {
			processedParam = flattenParams((Props) param);
		} else {
			processedParam = extractParamAsProps(param);
		}
		return processedParam;
	}

	// TODO Refactor this method because there are other method very similar to
	// this but with params instead result
	public Object flattenResult(Object result, RemoteObjectManager manager) {

		if (result == null) {
			return null;
		} else if (result instanceof Enum<?>) {
			return result.toString();
		} else if (isPrimitive(result)) {
			return result;
		} else if (result instanceof List<?>) {
			return flattenResultList((List<?>) result, manager);
		} else if (result.getClass().getAnnotation(RemoteClass.class) != null) {
			return extractObjectRefFromRemoteClass(result, manager);
		} else {
			return extractResultAsProps(result, manager);
		}

	}

	private Object extractObjectRefFromRemoteClass(Object result,
			RemoteObjectManager manager) {

		return manager.getObjectRefFrom(result);
	}

	// TODO Refactor this method because there are other method very similar to
	// this but with params instead result
	private Object extractResultAsProps(Object result,
			RemoteObjectManager manager) {

		Map<String, Object> propsMap = new HashMap<>();
		for (Method method : result.getClass().getMethods()) {

			String propName = null;

			String methodName = method.getName();
			if (methodName.startsWith("is")) {
				propName = methodName.substring(2, methodName.length());
			} else if (methodName.startsWith("get")
					&& !methodName.equals("getClass")) {
				propName = methodName.substring(3, methodName.length());
			}

			if (propName != null) {
				try {
					propName = Character.toLowerCase(propName.charAt(0))
							+ propName.substring(1);
					Object value = flattenResult(method.invoke(result), manager);
					propsMap.put(propName, value);

				} catch (Exception e) {
					log.warn(
							"Exception while accessing prop '{}' in param object: {}",
							propName, result, e);
				}
			}
		}

		return new Props(propsMap);
	}

	// TODO Refactor this method because there are other method very similar to
	// this but with params instead result
	private Object flattenResultList(List<?> resultList,
			RemoteObjectManager manager) {
		List<Object> plainResult = new ArrayList<>(resultList.size());
		for (Object result : resultList) {
			plainResult.add(flattenResult(result, manager));
		}
		return plainResult;
	}

	/**
	 * Extract the bean properties of this param as Props object.
	 * 
	 * @param param
	 * @return
	 */
	private Object extractParamAsProps(Object param) {

		Map<String, Object> propsMap = new HashMap<>();
		for (Method method : param.getClass().getMethods()) {

			String propName = null;

			String methodName = method.getName();
			if (methodName.startsWith("is")) {
				propName = methodName.substring(2, methodName.length());
			} else if (methodName.startsWith("get")
					&& !methodName.equals("getClass")) {
				propName = methodName.substring(3, methodName.length());
			}

			if (propName != null) {
				try {
					propName = Character.toLowerCase(propName.charAt(0))
							+ propName.substring(1);
					Object value = flattenParam(method.invoke(param));
					propsMap.put(propName, value);

				} catch (Exception e) {
					log.warn(
							"Exception while accessing prop '{}' in param object: {}",
							propName, param, e);
				}
			}
		}

		return new Props(propsMap);
	}

	private boolean isPrimitive(Object param) {
		return param instanceof String || param instanceof Boolean
				|| param instanceof Integer || param instanceof Float;
	}

	public Object[] unflattenParams(Annotation[][] paramAnnotations,
			Type[] paramTypes, Props params, ObjectRefsManager manager) {

		if (params == null) {
			return null;
		}

		Object[] returnParams = new Object[paramTypes.length];

		for (int i = 0; i < paramTypes.length; i++) {

			String paramName = ParamAnnotationUtils.getParamAnnotation(
					paramAnnotations[i]).value();
			Object value = params.getProp(paramName);
			returnParams[i] = unflattenValue(paramName, paramTypes[i], value,
					manager);
		}

		return returnParams;
	}

	public Object unflattenValue(String paramName, Type type, Object value,
			ObjectRefsManager manager) {

		if (type instanceof Class) {

			Class<?> clazz = (Class<?>) type;

			if (isPrimitiveClass(clazz)) {
				return value;
			} else if (clazz.isEnum()) {
				return unflattenEnumConstant(type, value, clazz);
			} else {

				if (value instanceof String) {
					return unflattenRemoteObject(type, (String) value, manager);

				} else if (value instanceof Props) {
					return unflattedComplexType(clazz, (Props) value, manager);

				} else {
					throw new ProtocolException(
							"A objectRef coded with a String or a Props is expected for param type '"
									+ type + "'");
				}
			}

		} else if (type instanceof ParameterizedType) {

			ParameterizedType pType = (ParameterizedType) type;
			if (((Class<?>) pType.getRawType()).isAssignableFrom(List.class)) {
				return unflattenList(paramName, (List<?>) value,
						pType.getActualTypeArguments()[0], manager);
			}
		}

		throw new ProtocolException("Type '" + type + "' is not supported");
	}

	private boolean isPrimitiveClass(Class<?> clazz) {
		return clazz == String.class || clazz == Boolean.class
				|| clazz == Float.class || clazz == Integer.class
				|| clazz == boolean.class || clazz == float.class
				|| clazz == int.class || clazz == void.class
				|| clazz == Void.class;
	}

	private Object unflattedComplexType(Class<?> clazz, Props props,
			ObjectRefsManager manager) {

		Constructor<?> constructor = clazz.getConstructors()[0];

		Object[] constParams = new Object[constructor.getParameterTypes().length];

		List<String> paramNames = ParamAnnotationUtils
				.getParamNames(constructor);
		Class<?>[] constClasses = constructor.getParameterTypes();

		for (int i = 0; i < constParams.length; i++) {
			String paramName = paramNames.get(i);
			constParams[i] = unflattenValue(paramName, constClasses[i],
					props.getProp(paramName), manager);
		}

		try {
			return constructor.newInstance(constParams);
		} catch (Exception e) {
			throw new ProtocolException(
					"Exception while creating an object for the class '"
							+ clazz.getSimpleName() + "'", e);
		}
	}

	private Object unflattenList(String paramName, List<?> value, Type type,
			ObjectRefsManager manager) {

		List<Object> list = new ArrayList<>();
		int counter = 0;
		for (Object object : value) {
			list.add(unflattenValue(paramName + "[" + counter + "]", type,
					object, manager));
			counter++;
		}
		return list;
	}

	private Object unflattenRemoteObject(Type type, String value,
			ObjectRefsManager manager) {

		Object remoteObject = manager.getObject(value);
		if (remoteObject == null) {

			if (manager instanceof RomClientObjectManager) {

				RomClientObjectManager clientManager = (RomClientObjectManager) manager;
				RemoteObject newRemoteObject = new RemoteObject(value,
						((Class<?>) type).getSimpleName(),
						clientManager.getClient(), clientManager);
				clientManager.registerObject(value, newRemoteObject);
				return newRemoteObject;

			}

			throw new ProtocolException("Remote object with objectRef '"
					+ value + "' is not found");

		} else if (remoteObject instanceof RemoteObject) {
			// We are in the client side
			Object wrapper = ((RemoteObject) remoteObject)
					.getWrapperForUnflatten();
			return (wrapper != null) ? wrapper : remoteObject;

		} else {
			return remoteObject;
		}

	}

	private Object unflattenEnumConstant(Type type, Object value, Class<?> clazz) {
		Object[] enumConsts = clazz.getEnumConstants();
		for (Object enumConst : enumConsts) {
			if (enumConst.toString().equals(value)) {
				return enumConst;
			}
		}
		// TODO Improve exception reporting
		throw new ProtocolException("Enum '" + value
				+ "' not found in enumType '" + type.toString() + "'");
	}

	public Type calculateFlattenType(Type type) {
		switch (getRomType(type)) {
		case BOOLEAN:
		case INTEGER:
		case FLOAT:
		case STRING:
		case VOID:
			return type;
		case CT_ENUM:
			return String.class;
		case CT_REGISTER:
			return Props.class;
		case LIST:
			return new GenericListType(
					calculateFlattenType(extractListType(type)));
		case REMOTE_CLASS:
			return String.class;
		default:
			throw new ProtocolException("Unknown type: " + type);
		}

	}

	private Type extractListType(Type type) {
		return ((ParameterizedType) type).getActualTypeArguments()[0];
	}

	public RomType getRomType(Type type) {
		if (type == Void.class || type == void.class) {
			return RomType.VOID;
		} else if (type == Integer.class || type == int.class) {
			return RomType.INTEGER;
		} else if (type == Float.class || type == float.class) {
			return RomType.FLOAT;
		} else if (type == Boolean.class || type == boolean.class) {
			return RomType.BOOLEAN;
		} else if (type == String.class) {
			return RomType.STRING;
		} else if (isEnum(type)) {
			return RomType.CT_ENUM;
		} else if (isComplexTypeRegister(type)) {
			return RomType.CT_REGISTER;
		} else if (isList(type)) {
			return RomType.LIST;
		} else if (isRemoteClass(type)) {
			return RomType.REMOTE_CLASS;
		} else {
			throw new ProtocolException("Unknown type: " + type);
		}
	}

	public boolean isRemoteClass(Type type) {
		return type instanceof Class
				&& ((Class<?>) type).getAnnotation(RemoteClass.class) != null;
	}

	public boolean isComplexTypeRegister(Type type) {
		return type instanceof Class
				&& ((Class<?>) type).getAnnotation(RemoteClass.class) == null
				&& !isEnum(type) && !isList(type);
	}

	public boolean isList(Type type) {
		return type instanceof Class
				&& ((Class<?>) type).isAssignableFrom(List.class)
				|| type instanceof ParameterizedType
				&& isList(((ParameterizedType) type).getRawType());
	}

	public boolean isEnum(Type type) {
		return type instanceof Class && ((Class<?>) type).isEnum();
	}

}
