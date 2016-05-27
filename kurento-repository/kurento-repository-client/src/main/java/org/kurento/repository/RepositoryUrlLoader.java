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

package org.kurento.repository;

import java.nio.file.Path;

import org.kurento.commons.UrlServiceLoader;

public class RepositoryUrlLoader extends UrlServiceLoader<RepositoryUrlProvider> {

  public static final String REPOSITORY_URL_PROPERTY = "repository.url";
  public static final String REPOSITORY_URL_PROVIDER_PROPERTY = "repository.url.provider";
  public static final String DEFAULT_REPOSITORY_URL = "http://localhost:7676";

  public RepositoryUrlLoader(Path configFile) {
    super(configFile, REPOSITORY_URL_PROPERTY, REPOSITORY_URL_PROVIDER_PROPERTY,
        DEFAULT_REPOSITORY_URL);
  }

  public String getRepositoryUrl() {
    if (getStaticUrl() == null) {
      return getServiceProvider().getRepositoryUrl();
    } else {
      return getStaticUrl();
    }
  }

}
