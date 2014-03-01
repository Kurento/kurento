package com.kurento.tool.rom.server;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.kurento.kmf.jsonrpcconnector.Props;
import com.kurento.tool.rom.ParamAnnotationUtils;

public class RomServer {

	private RemoteObjectManager manager = new RemoteObjectManager();

	private String packageName;
	private String classSuffix;

	public RomServer(String packageName, String classSuffix) {
		this.packageName = packageName;
		this.classSuffix = classSuffix;
	}

	public String create(String remoteClassType, Props constructorParams)
			throws RomException {

		try {

			Class<?> clazz = Class.forName(packageName + "." + remoteClassType
					+ classSuffix);

			Constructor<?> constructor = clazz.getConstructors()[0];

			Object[] unflattenedConstParams = unflattenParams(
					constructor.getParameterAnnotations(),
					constructor.getGenericParameterTypes(), constructorParams);

			Object object = constructor.newInstance(unflattenedConstParams);

			return manager.putObject(object);

		} catch (Exception e) {
			// TODO Improve exception reporting
			throw new RomException(
					"Exception while creating an object with remoteClass='"
							+ remoteClassType + "' and params="
							+ constructorParams, e);
		}
	}

	@SuppressWarnings("unchecked")
	public <E> E invoke(String objectRef, String methodName, Props params,
			Class<E> clazz) throws RomException {
		return (E) invoke(objectRef, methodName, params, (Type) clazz);
	}

	public Object invoke(String objectRef, String methodName, Props params,
			Type type) throws RomException {

		Object remoteObject = manager.getObject(objectRef);

		if (remoteObject == null) {
			throw new RomException("Invalid remote object reference");
		}

		Class<?> remoteObjClass = remoteObject.getClass();

		try {

			Method method = getMethod(remoteObjClass, methodName);

			Object[] unflattenedParams = unflattenParams(
					method.getParameterAnnotations(),
					method.getGenericParameterTypes(), params);

			return method.invoke(remoteObject, unflattenedParams);

		} catch (Exception e) {
			// TODO Improve exception reporting
			throw new RomException(
					"Invocation exception of object with remoteClass='"
							+ remoteObjClass.getSimpleName() + "', method="
							+ methodName + " and params=" + params, e);
		}
	}

	private Method getMethod(Class<?> remoteObjClass, String methodName) {
		for (Method method : remoteObjClass.getMethods()) {
			if (method.getName().equals(methodName)) {
				return method;
			}
		}
		throw new RuntimeException("Method '" + methodName
				+ "' not found in class '"
				+ remoteObjClass.getClass().getSimpleName() + "'");
	}

	private Object[] unflattenParams(Annotation[][] paramAnnotations,
			Type[] paramTypes, Props params) {

		if (params == null) {
			return null;
		}

		Object[] returnParams = new Object[paramTypes.length];

		for (int i = 0; i < paramTypes.length; i++) {

			String paramName = ParamAnnotationUtils.getParamAnnotation(
					paramAnnotations[i]).value();
			Object value = params.getProp(paramName);
			returnParams[i] = unflattenValue(paramName, paramTypes[i], value);
		}

		return returnParams;
	}

	private Object unflattenValue(String paramName, Type type, Object value) {

		if (type instanceof Class) {

			Class<?> clazz = (Class<?>) type;

			if (isPrimitiveClass(clazz)) {
				return value;
			} else if (clazz.isEnum()) {
				return unflattenEnumConstant(type, value, clazz);
			} else {

				if (value instanceof String) {
					return unflattenRemoteObject((String) value);

				} else if (value instanceof Props) {
					return unflattedComplexType(clazz, (Props) value);

				} else {
					// TODO Improve exception reporting
					throw new RuntimeException(
							"A objectRef coded with a String or a Props is expected for param type '"
									+ type + "'");
				}
			}

		} else if (type instanceof ParameterizedType) {

			ParameterizedType pType = (ParameterizedType) type;
			if (((Class<?>) pType.getRawType()).isAssignableFrom(List.class)) {
				return unflattenList(paramName, (List<?>) value,
						pType.getActualTypeArguments()[0]);
			}
		}

		// TODO Improve exception reporting
		throw new RuntimeException("Type '" + type + "' is not supported");
	}

	private boolean isPrimitiveClass(Class<?> clazz) {
		return clazz == String.class || clazz == Boolean.class
				|| clazz == Float.class || clazz == Integer.class
				|| clazz == boolean.class || clazz == float.class
				|| clazz == int.class;
	}

	private Object unflattedComplexType(Class<?> clazz, Props props) {

		Constructor<?> constructor = clazz.getConstructors()[0];

		Object[] constParams = new Object[constructor.getParameterTypes().length];

		List<String> paramNames = ParamAnnotationUtils
				.getParamNames(constructor);
		Class<?>[] constClasses = constructor.getParameterTypes();

		for (int i = 0; i < constParams.length; i++) {
			String paramName = paramNames.get(i);
			constParams[i] = unflattenValue(paramName, constClasses[i],
					props.getProp(paramName));
		}

		try {
			return constructor.newInstance(constParams);
		} catch (Exception e) {
			throw new RuntimeException(
					"Exception while creating an object for the class '"
							+ clazz.getSimpleName() + "'", e);
		}
	}

	private Object unflattenList(String paramName, List<?> value, Type type) {

		List<Object> list = new ArrayList<Object>();
		int counter = 0;
		for (Object object : value) {
			list.add(unflattenValue(paramName + "[" + counter + "]", type,
					object));
			counter++;
		}
		return list;
	}

	private Object unflattenRemoteObject(String value) {

		Object remoteObject = manager.getObject(value);
		if (remoteObject == null) {
			// TODO Improve exception reporting
			throw new RuntimeException("Remote object with objectRef '" + value
					+ "' is not found");
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
		throw new RuntimeException("Enum '" + value
				+ "' not found in enumType '" + type.toString() + "'");
	}

	public void release(String objectRef) {
		this.manager.releaseObject(objectRef);
	}
}
