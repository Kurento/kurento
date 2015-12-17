
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
    if (getUrl() == null) {
      return getServiceProvider().getRepositoryUrl();
    } else {
      return getUrl();
    }
  }

}
