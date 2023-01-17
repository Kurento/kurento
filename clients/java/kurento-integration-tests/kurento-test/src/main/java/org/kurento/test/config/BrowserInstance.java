/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.test.config;

import java.util.List;
import java.util.Map;

import org.kurento.test.browser.BrowserType;
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
  private String node;
  private String login;
  private String passwd;
  private String key;
  private String protocol;
  private String path;
  private int port;
  private boolean enableScreenCapture;
  private List<Map<String, String>> extensions;
  private String saucelabsUser;
  private String saucelabsKey;
  private String seleniumVersion;
  private String host;
  private String video;
  private boolean avoidProxy;
  private String parentTunnel;

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

  public String getNode() {
    return node;
  }

  public void setNode(String node) {
    this.node = node;
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

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public boolean isEnableScreenCapture() {
    return enableScreenCapture;
  }

  public void setEnableScreenCapture(boolean enableScreenCapture) {
    this.enableScreenCapture = enableScreenCapture;
  }

  public boolean isLocal() {
    return BrowserScope.LOCAL.toString().equals(this.getScope());
  }

  public boolean isRemote() {
    return BrowserScope.REMOTE.toString().equals(this.getScope());
  }

  public boolean isDocker() {
    return BrowserScope.DOCKER.toString().equals(this.getScope());
  }

  public boolean isSauceLabs() {
    return BrowserScope.SAUCELABS.toString().equals(this.getScope());
  }

  public boolean isElastest() {
    return BrowserScope.ELASTEST.toString().equals(this.getScope());
  }

  public BrowserType getBrowserType() {
    return BrowserType.valueOf(getBrowser().toUpperCase());
  }

  public Platform getPlatformType() {
    return Platform.valueOf(getPlatform().toUpperCase());
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getSaucelabsUser() {
    return saucelabsUser;
  }

  public void setSaucelabsUser(String saucelabsUser) {
    this.saucelabsUser = saucelabsUser;
  }

  public String getSeleniumVersion() {
    return seleniumVersion;
  }

  public void setSeleniumVersion(String seleniumVersion) {
    this.seleniumVersion = seleniumVersion;
  }

  public String getSaucelabsKey() {
    return saucelabsKey;
  }

  public void setSaucelabsKey(String saucelabsKey) {
    this.saucelabsKey = saucelabsKey;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public String getVideo() {
    return video;
  }

  public void setVideo(String video) {
    this.video = video;
  }

  public boolean isAvoidProxy() {
    return avoidProxy;
  }

  public void setAvoidProxy(boolean avoidProxy) {
    this.avoidProxy = avoidProxy;
  }

  public String getParentTunnel() {
    return parentTunnel;
  }

  public void setParentTunnel(String parentTunnel) {
    this.parentTunnel = parentTunnel;
  }

  public List<Map<String, String>> getExtensions() {
    return extensions;
  }

  public void setExtensions(List<Map<String, String>> extensions) {
    this.extensions = extensions;
  }

  @Override
  public String toString() {
    return "Browser [scope=" + scope + ", browser=" + browser + ", version=" + version
        + ", platform=" + platform + ", instances=" + instances + "]";
  }

}
