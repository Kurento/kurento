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
 */

package org.kurento.repository;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.StandardSystemProperty;

import retrofit.RestAdapter;

/**
 * Factory to create {@link RepositoryClient} that can be used to access services offered by a
 * repository server application. It requires the repository's service URL for REST communications,
 * which can be configured by means of reading properties from well-known locations (see
 * {@link RepositoryUrlLoader}), or directly when instantiating the provider.
 *
 * @author <a href="mailto:micael.gallego@gmail.com">Micael Gallego</a>
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class RepositoryClientProvider {

  private static final Logger log = LoggerFactory.getLogger(RepositoryClientProvider.class);

  private static RepositoryUrlLoader repositoryUrlLoader;

  private RepositoryClient restService;

  /**
   * Creates a provider for clients to a server instance. The repository URL is searched for in
   * properties from well-known locations.
   *
   * @return a factory of {@link RepositoryClient}
   */
  public static RepositoryClientProvider createProvider() {
    return createProvider(getRepositoryUrl());
  }

  /**
   * Creates a provider for clients to a server instance.
   *
   * @param repoRestUrl
   *          the repository server URL (where the server expects REST connections)
   * @return a factory of {@link RepositoryClient}
   */
  public static RepositoryClientProvider createProvider(String repoRestUrl) {
    RepositoryClientProvider provider = new RepositoryClientProvider();
    RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint(repoRestUrl).build();
    provider.restService = restAdapter.create(RepositoryClient.class);
    log.debug("Rest client service created for {}", repoRestUrl);
    return provider;
  }

  /**
   * Creates a client to a server instance. The repository URL of the client provider is searched
   * for in properties from well-known locations.
   *
   * @return a {@link RepositoryClient}
   */
  public static RepositoryClient create() {
    return create(getRepositoryUrl());
  }

  /**
   * Creates a client to a server instance found on the provided URL.
   *
   * @param repoRestUrl
   *          the repository server URL (where the server expects REST connections)
   * @return a {@link RepositoryClient}
   */
  public static RepositoryClient create(String repoRestUrl) {
    return createProvider(repoRestUrl).getRepositoryClient();
  }

  private RepositoryClientProvider() {
  }

  /**
   * Gets the repository client.
   *
   * @return a {@link RepositoryClient} to communicate with a repository server instance
   */
  public RepositoryClient getRepositoryClient() {
    return restService;
  }

  private static synchronized String getRepositoryUrl() {

    if (repositoryUrlLoader == null) {

      Path configFile =
          Paths.get(StandardSystemProperty.USER_HOME.value(), ".kurento", "config.properties");

      repositoryUrlLoader = new RepositoryUrlLoader(configFile);
    }

    return repositoryUrlLoader.getRepositoryUrl();
  }
}
