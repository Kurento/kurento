/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
package org.kurento.client.test;

import org.kurento.test.base.KurentoClientTest;

/**
 * Base for API tests.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class ApiBase extends KurentoClientTest {

	public static final String URL_BARCODES = "http://files.kurento.org/video/filter/barcodes.webm";
	public static final String URL_FIWARECUT = "http://files.kurento.org/video/filter/fiwarecut.webm";
	public static final String URL_SMALL = "http://files.kurento.org/video/format/small.webm";
	public static final String URL_PLATES = "http://files.kurento.org/video/filter/plates.webm";
	public static final String URL_POINTER_DETECTOR = "http://files.kurento.org/video/filter/pointerDetector.mp4";

}
