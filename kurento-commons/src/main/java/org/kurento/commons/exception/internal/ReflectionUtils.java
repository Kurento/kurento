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
package org.kurento.commons.exception.internal;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

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
	private static ReflectionUtils singleton;

	/**
	 * Reflections scans the classpath, indexes the metadata, allows to query it
	 * on runtime and may save and collect that information.
	 */
	private final Reflections reflections;

	/**
	 * Constructor; it instantiates the Reflections (
	 * <code>org.reflections.Reflections</code>) object.
	 */
	private ReflectionUtils() {
		// Former Reflections instantiation:
		// this.reflections = new Reflections("", new TypeAnnotationsScanner());

		List<URL> urls = new ArrayList<>();
		for (URL url : ClasspathHelper.forManifest(ClasspathHelper
				.forClassLoader())) {
			if (url.toString().toLowerCase().endsWith(".jar")) {
				urls.add(url);
			}
		}
		this.reflections = new Reflections(new ConfigurationBuilder()
				.setScanners(new TypeAnnotationsScanner())
				.setUrls(ClasspathHelper.forPackage("")).addUrls(urls));
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
