/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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

package org.kurento.commons;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class UrlServiceLoader<P> {

  private static final Logger log = LoggerFactory.getLogger(UrlServiceLoader.class);

  private String urlProperty;
  private String urlProviderProperty;
  private String defaultUrl;

  private String serviceProviderClassName;
  private String staticUrl;

  private P serviceProvider;

  public UrlServiceLoader(Path configFile, String urlProperty, String urlProviderProperty,
      String defaultUrl) {

    this.urlProperty = urlProperty;
    this.urlProviderProperty = urlProviderProperty;
    this.defaultUrl = defaultUrl;

    staticUrl = load(configFile);
  }

  private String load(Path configFile) {

    String kmsUrlInProperty = System.getProperty(urlProperty);
    if (kmsUrlInProperty != null && !kmsUrlInProperty.equals("")) {
      return kmsUrlInProperty;
    }

    try {

      if (configFile != null && Files.exists(configFile)) {

        Properties properties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
          properties.load(reader);
        }

        String kmsUrl = properties.getProperty(urlProperty);

        if (kmsUrl != null) {
          log.debug("Using static url from property {}={} configured in config file {}", urlProperty,
              kmsUrl, configFile.toAbsolutePath());
          return kmsUrl;
        }

        serviceProviderClassName = properties.getProperty(urlProviderProperty);

        if (serviceProviderClassName == null) {
          log.warn("The file {} lacks property '{}' or '{}'. The default url '{}' will be used",
              configFile.toAbsolutePath(), urlProviderProperty, urlProperty, defaultUrl);

          return defaultUrl;
        } else {
          log.debug("Using UrlServiceProvider={} configured in config file {}",
              serviceProviderClassName, configFile.toAbsolutePath());
          return null;
        }

      } else {
        log.debug(
            "Config file is null (usually this means that config file doesn't exist). Using default url {}",
            defaultUrl);
        return defaultUrl;
      }

    } catch (Exception e) {
      log.warn("Exception loading {} file. Returning default kmsUri='{}'", configFile, defaultUrl,
          e);

      return defaultUrl;
    }
  }

  protected String getStaticUrl() {
    return staticUrl;
  }

  @SuppressWarnings("unchecked")
  private P createUrlProvider() {
    try {

      Class<?> providerClass = Class.forName(serviceProviderClassName);

      return (P) providerClass.newInstance();

    } catch (Exception e) {
      throw new RuntimeException("Exception loading url provider class " + serviceProviderClassName,
          e);
    }
  }

  protected P getServiceProvider() {
    if (serviceProvider == null) {
      serviceProvider = createUrlProvider();
    }
    return serviceProvider;
  }

}
