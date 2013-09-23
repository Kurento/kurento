package com.kurento.kmf.common.excption.internal;

import java.lang.annotation.Annotation;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;

/**
 * Annotation singleton utility class; it uses
 * <code>org.reflections.Reflections</code> to look reflexively for annotated
 * objects within the classpath.
 * 
 * @see <a href="https://code.google.com/p/reflections/">Reflections</a>
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 * 
 */
public class ReflectionUtils {

	/**
	 * Singleton.
	 */
	private static ReflectionUtils singleton = null;

	/**
	 * Reflections scans the classpath, indexes the metadata, allows to query it
	 * on runtime and may save and collect that information.
	 */
	private Reflections reflections;

	/**
	 * Constructor; it instantiates the Reflections (
	 * <code>org.reflections.Reflections</code>) object.
	 */
	private ReflectionUtils() {
		this.reflections = new Reflections("", new TypeAnnotationsScanner());
	}

	/**
	 * Singleton accessor (getter).
	 * 
	 * @return ExceptionsUtil singleton
	 */
	public static ReflectionUtils getSingleton() {
		if (singleton == null) {
			singleton = new ReflectionUtils();
		}
		return singleton;
	}

	/**
	 * Returns the set of classes within the classpath annotated with a given
	 * annotation class, passed as a parameter.
	 * 
	 * @param annotation
	 *            Annotation to look reflexively for annotated objects in the
	 *            classpath
	 * @return Set of classes which has been annotated
	 */
	public static Set<Class<?>> getTypesAnnotatedWith(
			Class<? extends Annotation> annotation) {
		return ReflectionUtils.getSingleton().reflections
				.getTypesAnnotatedWith(annotation);
	}

}