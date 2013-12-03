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
package com.kurento.kmf.media;

/**
 * Filter that detects faces in a video feed. Those on the right half of the
 * feed are overlaid with a pirate hat, and those on the left half are covered
 * by a Darth Vader helmet. This is an example filter, intended to demonstrate
 * how to integrate computer vision capabilities into the multimedia
 * infrastructure.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public interface JackVaderFilter extends Filter {

	/**
	 * Builder for the {@link JackVaderFilter}.
	 * 
	 * @author Ivan Gracia (igracia@gsyc.es)
	 * @since 2.0.0
	 */
	public interface JackVaderFilterBuilder extends
			MediaObjectBuilder<JackVaderFilterBuilder, JackVaderFilter> {

	}

}
