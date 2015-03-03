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
package org.kurento.test.config;

import org.kurento.test.client.BrowserType;
import org.openqa.selenium.Platform;

/**
 * Browser instance. POJO class for parsing JSON files using the GSON library.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.1.0
 */
public class BrowserInstance {

	private String scope;
	private String browser;
	private String version;
	private String platform;
	private int instances;
	private int browserPerInstance;
	private String hostAddress;
	private String login;
	private String passwd;
	private String key;

	public BrowserInstance(String browser) {
		this.browser = browser;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public String getBrowser() {
		return browser;
	}

	public void setBrowser(String browser) {
		this.browser = browser;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public int getInstances() {
		return instances;
	}

	public void setInstances(int instances) {
		this.instances = instances;
	}

	public int getBrowserPerInstance() {
		return browserPerInstance;
	}

	public void setBrowserPerInstance(int browserPerInstance) {
		this.browserPerInstance = browserPerInstance;
	}

	public String getHostAddress() {
		return hostAddress;
	}

	public void setHostAddress(String hostAddress) {
		this.hostAddress = hostAddress;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getPasswd() {
		return passwd;
	}

	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public boolean isLocal() {
		return BrowserScope.LOCAL.toString().equals(this.getScope());
	}

	public boolean isRemote() {
		return BrowserScope.REMOTE.toString().equals(this.getScope());
	}

	public boolean isSauceLabs() {
		return BrowserScope.SAUCELABS.toString().equals(this.getScope());
	}

	public BrowserType getBrowserType() {
		return BrowserType.valueOf(getBrowser().toUpperCase());
	}

	public Platform getPlatformType() {
		return Platform.valueOf(getPlatform().toUpperCase());
	}

	@Override
	public String toString() {
		return "Browser [scope=" + scope + ", browser=" + browser
				+ ", version=" + version + ", platform=" + platform
				+ ", instances=" + instances + "]";
	}

}
