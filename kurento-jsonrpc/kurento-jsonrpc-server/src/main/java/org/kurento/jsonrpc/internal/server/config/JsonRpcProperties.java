/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

package org.kurento.jsonrpc.internal.server.config;

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
   *          the keystoneHost to set
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
   *          the keystonePort to set
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
   *          the keystoneOAuthVersionPath to set
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
   *          the keystonePath to set
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
   *          the proxyUser to set
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
   *          the proxyPass to set
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
   *          the authToken to set
   */
  public void setAuthToken(String authToken) {
    this.proxyToken = authToken;
  }

}
