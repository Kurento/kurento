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
package com.kurento.kmf.test;

/**
 * Port manager for HTTP server; if not defined, it will be 7779.
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */
public class PortManager {

	public static int getPort() {
		String port = System.getProperty("http.port");
		if (port == null) {
			return 7779;
		} else {
			return Integer.parseInt(port);
		}
	}

}
