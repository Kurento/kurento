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
 * @since 4.1.1
 * 
 */
public class JsonRpcProperties {

	private String keystoneHost; // "http://cloud.lab.fi-ware.org";

	private int keystonePort = 4731;

	private String oAuthVersion = "v2.0";

	private String Path = '/' + oAuthVersion + "/access-tokens/";

	private String proxyUser = "pepProxy";

	private String proxyPass = "pepProxy";

	private String proxyToken;

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
	 * @return the keystoneOAuthVersionPath
	 */
	public String getOAuthVersion() {
		return oAuthVersion;
	}

	/**
	 * @param keystoneOAuthVersionPath
	 *            the keystoneOAuthVersionPath to set
	 */
	public void setKeystoneOAuthVersionPath(String keystoneOAuthVersionPath) {
		this.oAuthVersion = keystoneOAuthVersionPath;
	}

	/**
	 * @return the keystonePath
	 */
	public String getKeystonePath() {
		return Path;
	}

	/**
	 * @param keystonePath
	 *            the keystonePath to set
	 */
	public void setKeystonePath(String keystonePath) {
		this.Path = keystonePath;
	}

	/**
	 * @return the proxyUser
	 */
	public String getKeystoneProxyUser() {
		return proxyUser;
	}

	/**
	 * @param proxyUser
	 *            the proxyUser to set
	 */
	public void setKeystoneProxyUser(String proxyUser) {
		this.proxyUser = proxyUser;
	}

	/**
	 * @return the proxyPass
	 */
	public String getKeystoneProxyPass() {
		return proxyPass;
	}

	/**
	 * @param proxyPass
	 *            the proxyPass to set
	 */
	public void setKeystoneProxyPass(String proxyPass) {
		this.proxyPass = proxyPass;
	}

	/**
	 * @return the authToken
	 */
	public String getAuthToken() {
		return proxyToken;
	}

	/**
	 * @param authToken
	 *            the authToken to set
	 */
	public void setAuthToken(String authToken) {
		this.proxyToken = authToken;
	}
}
