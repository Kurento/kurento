package org.kurento.client.internal.transport.serialization;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.client.internal.server.ProtocolException;

public class ModuleClassesManager {

	private final ConcurrentHashMap<String, String> packageNamesByModuleName = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Class<?>> classesByClassName = new ConcurrentHashMap<>();

	public Class<?> getClassFor(String fullyTypeName) {
		String[] parts = fullyTypeName.split("\\.");
		return getClassFor(parts[0], parts[1]);
	}

	public Class<?> getClassFor(String moduleName, String typeName) {

		Objects.requireNonNull(typeName, "typeName must not be null");
		Objects.requireNonNull(moduleName, "moduleName must not be null");

		try {

			String packageName = packageNamesByModuleName.get(moduleName);

			if (packageName == null) {

				packageName = getPackageNameWithModuleInfoClass(moduleName);

				packageNamesByModuleName.put(moduleName, packageName);
			}

			String className = packageName + "." + typeName;

			Class<?> clazz = classesByClassName.get(className);

			if (clazz == null) {
				clazz = Class.forName(className);
				classesByClassName.put(className, clazz);
			}

			return clazz;

		} catch (Exception e) {
			throw new ProtocolException("Exception creating Java Class for '"
					+ moduleName + "." + typeName + "'", e);
		}
	}

	private String getPackageNameWithModuleInfoClass(String moduleName)
			throws ClassNotFoundException, NoSuchMethodException,
			IllegalAccessException, InvocationTargetException {

		String moduleInfoClassName = getModuleInfoClassName(moduleName);
		Class<?> clazzPackage = Class.forName(moduleInfoClassName);
		Method method = clazzPackage.getMethod("getPackageName");
		return (String) method.invoke(clazzPackage);
	}

	private String getModuleInfoClassName(String moduleName) {

		String moduleNameWithFirstUpper = moduleName.substring(0, 1)
				.toUpperCase() + moduleName.substring(1, moduleName.length());

		String classPackageName = "org.kurento.module."
				+ moduleNameWithFirstUpper + "ModuleInfo";

		return classPackageName;
	}

}
