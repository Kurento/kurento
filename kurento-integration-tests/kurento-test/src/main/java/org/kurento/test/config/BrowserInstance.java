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
	private int number;

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

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public boolean isLocal() {
		return BrowserScope.LOCAL.equals(this.getScope());
	}

	public boolean isRemote() {
		return BrowserScope.REMOTE.equals(this.getScope());
	}

	public boolean isSauceLabs() {
		return BrowserScope.REMOTE.equals(this.getScope());
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
				+ ", number=" + number + "]";
	}

}
