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
package com.kurento.kmf.orion;

/**
 * @author Ivan Gracia (igracia@naevatec.com)
 * 
 */
public class OrionConnectorConfiguration {

	private String orionHost = "130.206.81.109";
	private int orionPort = 1026;
	private String orionScheme = "http";

	/**
	 * @return the orionHost
	 */
	public String getOrionHost() {
		return orionHost;
	}

	/**
	 * @param orionHost
	 *            the orionHost to set
	 */
	public void setOrionHost(String orionHost) {
		this.orionHost = orionHost;
	}

	/**
	 * @return the orionPort
	 */
	public int getOrionPort() {
		return orionPort;
	}

	/**
	 * @param orionPort
	 *            the orionPort to set
	 */
	public void setOrionPort(int orionPort) {
		this.orionPort = orionPort;
	}

	/**
	 * @return the orionSchema
	 */
	public String getOrionScheme() {
		return orionScheme;
	}

	/**
	 * @param orionSchema
	 *            the orionSchema to set
	 */
	public void setOrionScheme(String orionSchema) {
		this.orionScheme = orionSchema;
	}
}
