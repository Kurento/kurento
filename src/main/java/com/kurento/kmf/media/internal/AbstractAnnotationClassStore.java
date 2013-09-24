package com.kurento.kmf.media.internal;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import com.kurento.kmf.common.excption.internal.ReflectionUtils;

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
			annotatedClassMap.put(getTypeFromAnnotation(clazz),
					(Class<?>) clazz);
		}
	}

	protected abstract String getTypeFromAnnotation(Class<?> clazz);

	Class<?> get(String key) {
		return annotatedClassMap.get(key);
	}
}
