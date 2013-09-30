/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
package com.kurento.kmf.media.internal.spring;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import com.kurento.kmf.common.excption.internal.ReflectionUtils;

/**
 * This class contains a map of classes annotated by T. The annotation used as
 * param T MUST have a field named type, which will be used as key for the
 * annotated class. All key strings are converted into their lower-case
 * equivalents.
 * 
 * @author Ivan Gracia
 * 
 * @param <T>
 */
public abstract class AbstractAnnotationClassStore<T extends Annotation> {

	protected final Map<String, Class<?>> annotatedClassMap = new HashMap<String, Class<?>>();
	private final Class<T> annotation;

	AbstractAnnotationClassStore(Class<T> annotation) {
		this.annotation = annotation;
	}

	@PostConstruct
	private void init() {
		Set<Class<?>> annotatedClassSet = ReflectionUtils
				.getTypesAnnotatedWith(annotation);
		for (Class<?> clazz : annotatedClassSet) {
			annotatedClassMap.put(getTypeFromAnnotation(clazz).toLowerCase(),
					clazz);
		}
	}

	protected abstract String getTypeFromAnnotation(Class<?> clazz);

	/**
	 * Returns a class annotated by T. </br><strong>NOTE</strong></br> The class
	 * store is case insensitive.
	 * 
	 * @param key
	 * @return
	 */
	Class<?> get(String key) {
		return annotatedClassMap.get(key.toLowerCase());
	}
}
