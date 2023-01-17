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

package org.kurento.client.internal;

import java.nio.file.Path;

import org.kurento.commons.UrlServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KmsUrlLoader extends UrlServiceLoader<KmsProvider> {

  private static final Logger log = LoggerFactory.getLogger(KmsUrlLoader.class);

  public static final String KMS_URL_PROPERTY = "kms.url";
  public static final String KMS_URL_PROVIDER_PROPERTY = "kms.url.provider";
  public static final String DEFAULT_KMS_URL = "ws://127.0.0.1:8888/kurento";

  public KmsUrlLoader(Path configFile) {
    super(configFile, KMS_URL_PROPERTY, KMS_URL_PROVIDER_PROPERTY, DEFAULT_KMS_URL);
  }

  public String getKmsUrl(String id) {
    log.debug("Executing getKmsUrlLoad({}) in KmsUrlLoader", id);
    if (getStaticUrl() == null) {
      log.debug("Obtaining kmsUrl from provider");
      return loadKmsUrlFromProvider(id, -1);
    } else {
      log.debug("Obtaining kmsUrl={} from config file or system property", getStaticUrl());
      return getStaticUrl();
    }
  }

  public String getKmsUrlLoad(String id, int loadPoints) {
    log.debug("Executing getKmsUrlLoad({},{}) in KmsUrlLoader", id, loadPoints);
    if (getStaticUrl() == null) {
      log.debug("Obtaining kmsUrl from provider");
      return loadKmsUrlFromProvider(id, loadPoints);
    } else {
      log.debug("Obtaining kmsUrl={} from config file or system property", getStaticUrl());
      return getStaticUrl();
    }
  }

  private synchronized String loadKmsUrlFromProvider(String id, int loadPoints) {
    log.debug("Executing loadKmsUrlFromProvider({},{}) in KmsUrlLoader", id, loadPoints);
    KmsProvider kmsProvider = getServiceProvider();
    if (loadPoints == -1) {
      String kmsUrl = kmsProvider.reserveKms(id);
      log.debug("Executed reserveKms({}) in serviceProvider with result={}", id, kmsUrl);
      return kmsUrl;
    } else {
      String kmsUrl = kmsProvider.reserveKms(id, loadPoints);
      log.debug("Executed reserveKms({},{}) in serviceProvider with result={}", id, loadPoints,
          kmsUrl);
      return kmsUrl;
    }
  }

  public void clientDestroyed(String id) {
    log.debug("Executing clientDestroyed({}) in KmsUrlLoader", id);
    if (getStaticUrl() == null) {
      getServiceProvider().releaseKms(id);
      log.debug("Executed releaseKms({}) in serviceProvider", id);
    }
  }

}
