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
package com.kurento.kmf.jsonrpcconnector.internal.server.config;

/**
 * Properties of the JSON RPC connector
 * 
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 3.0.7
 * 
 */
public class JsonRpcProperties {

	private String keystoneHost = "";// "http://cloud.lab.fi-ware.org";

	private int keystonePort = 4731;

	private String keystonePath = "/v2.0/tokens/";

	private String kurentoUsername = "pepProxy";

	private String kurentoPassword = "pepProxy";

	private String authToken = "7a5001779cfdd301e5174339c101e661";

	/**
	 * @return the keystoneHost
	 */
	public String getKeystoneHost() {
		return keystoneHost;
	}

	/**
	 * @param keystoneHost
	 *            the keystoneHost to set
	 */
	public void setKeystoneHost(String keystoneHost) {
		this.keystoneHost = keystoneHost;
	}

	/**
	 * @return the keystonePort
	 */
	public int getKeystonePort() {
		return keystonePort;
	}

	/**
	 * @param keystonePort
	 *            the keystonePort to set
	 */
	public void setKeystonePort(int keystonePort) {
		this.keystonePort = keystonePort;
	}

	/**
	 * @return the keystonePath
	 */
	public String getKeystonePath() {
		return keystonePath;
	}

	/**
	 * @param keystonePath
	 *            the keystonePath to set
	 */
	public void setKeystonePath(String keystonePath) {
		this.keystonePath = keystonePath;
	}

	/**
	 * @return the kurentoUsername
	 */
	public String getKurentoUsername() {
		return kurentoUsername;
	}

	/**
	 * @param kurentoUsername
	 *            the kurentoUsername to set
	 */
	public void setKurentoUsername(String kurentoUsername) {
		this.kurentoUsername = kurentoUsername;
	}

	/**
	 * @return the kurentoPassword
	 */
	public String getKurentoPassword() {
		return kurentoPassword;
	}

	/**
	 * @param kurentoPassword
	 *            the kurentoPassword to set
	 */
	public void setKurentoPassword(String kurentoPassword) {
		this.kurentoPassword = kurentoPassword;
	}

	/**
	 * @return the authToken
	 */
	public String getAuthToken() {
		return authToken;
	}

	/**
	 * @param authToken
	 *            the authToken to set
	 */
	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}
}
