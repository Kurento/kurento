/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.kurento.test.services;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.kurento.test.base.KurentoTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test watcher for tests.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class KurentoTestWatcher extends TestWatcher {

	public static Logger log = LoggerFactory
			.getLogger(KurentoTestWatcher.class);

	private static boolean succees = false;

	@Override
	protected void starting(Description description) {
		String methodName = description.getMethodName();

		logMessage("|       TEST STARTING: " + description.getClassName() + "."
				+ methodName);

		KurentoTest.setTestMethodName(methodName);
	}

	@Override
	protected void succeeded(Description description) {
		logMessage("|       TEST SUCCEEDED: " + description.getClassName() + "."
				+ description.getMethodName());

		super.succeeded(description);
		succees = true;
	}

	@Override
	protected void failed(Throwable e, Description description) {
		logMessage("|       TEST FAILED: " + description.getClassName() + "."
				+ description.getMethodName());

		invokeMethodsAnnotatedWith(FailedTest.class, description.getTestClass(),
				e, description);
	}

	@Override
	protected void finished(Description description) {
		super.finished(description);

		invokeMethodsAnnotatedWith(FinishedTest.class,
				description.getTestClass(), null, description);
	}

	private void invokeMethodsAnnotatedWith(
			Class<? extends Annotation> annotation, Class<?> testClass,
			Throwable throwable, Description description) {
		List<Method> methods = getMethodsAnnotatedWith(testClass, annotation);
		invokeMethods(methods, annotation, throwable, description);
	}

	public static boolean isSuccees() {
		return succees;
	}

	public void invokeMethods(List<Method> methods,
			Class<? extends Annotation> annotation, Throwable throwable,
			Description description) {
		for (Method method : methods) {
			log.debug("Invoking method {} annotated with {}", method,
					annotation);

			try {
				if (!Modifier.isPublic(method.getModifiers())) {
					log.warn("Method {} is not public and it cannot be invoked",
							method);
					continue;
				}

				if (!Modifier.isStatic(method.getModifiers())) {
					log.warn("Method {} is not static and it cannot be invoked",
							method);
					continue;
				}

				Class<?>[] parameterTypes = method.getParameterTypes();

				switch (parameterTypes.length) {
				case 0:
					method.invoke(null);
					break;

				case 1:
					if (parameterTypes[0].equals(Throwable.class)) {
						method.invoke(null, throwable);
					} else if (parameterTypes[0].equals(Description.class)) {
						method.invoke(null, description);
					} else {
						log.warn(
								"Method {} annotated with {} cannot be invoked."
										+ " Incorrect argument: {}",
								method, annotation, parameterTypes[0]);
					}
					break;

				case 2:
					Object param1 = parameterTypes[0].equals(Throwable.class)
							? throwable
							: parameterTypes[0].equals(Description.class)
									? description : null;
					Object param2 = parameterTypes[1].equals(Throwable.class)
							? throwable
							: parameterTypes[1].equals(Description.class)
									? description : null;

					if (param1 != null && param2 != null) {
						method.invoke(null, param1, param2);
					} else {
						log.warn(
								"Method {} annotated with {} cannot be invoked."
										+ " Incorrect arguments: {}, {}",
								method, annotation, parameterTypes[0],
								parameterTypes[1]);
					}
					break;

				default:
					log.warn(
							"Method {} annotated with {} cannot be invoked."
									+ " Incorrect arguments: {}",
							method, annotation,
							Arrays.toString(parameterTypes));
					break;
				}

			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				log.warn(
						"Exception invoking method {} annotated with {}: {} {}",
						method, e.getClass(), e.getMessage());
			}
		}
	}

	public List<Method> getMethodsAnnotatedWith(Class<?> clazz,
			Class<? extends Annotation> annotation) {
		List<Method> methods = new ArrayList<>();
		while (clazz != Object.class) {
			for (Method method : clazz.getDeclaredMethods()) {
				if (method.isAnnotationPresent(annotation)) {
					methods.add(method);
				}
			}
			clazz = clazz.getSuperclass();
		}
		return methods;
	}

	private void logMessage(String message) {
		log.info(KurentoTest.SEPARATOR);
		log.info(message);
		log.info(KurentoTest.SEPARATOR);
	}

}
