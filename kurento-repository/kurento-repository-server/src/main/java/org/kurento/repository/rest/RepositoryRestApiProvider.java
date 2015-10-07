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

package org.kurento.repository.rest;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.StandardSystemProperty;

import retrofit.RestAdapter;

public class RepositoryRestApiProvider {

	private static final Logger log = LoggerFactory
			.getLogger(RepositoryRestApiProvider.class);

	private static RepositoryUrlLoader repositoryUrlLoader;

	private RepositoryRestApi restService;

	public static RepositoryRestApiProvider createProvider() {
		return createProvider(getRepositoryUrl());
	}

	public static RepositoryRestApi create() {
		return create(getRepositoryUrl());
	}

	public static RepositoryRestApi create(String repoRestUrl) {
		return createProvider(repoRestUrl).getRestService();
	}

	public static RepositoryRestApiProvider createProvider(String repoRestUrl) {
		RepositoryRestApiProvider provider = new RepositoryRestApiProvider();
		RestAdapter restAdapter = new RestAdapter.Builder()
				.setEndpoint(repoRestUrl).build();
		provider.restService = restAdapter.create(RepositoryRestApi.class);
		log.info("Rest client service created for {}", repoRestUrl);
		return provider;
	}

	private RepositoryRestApiProvider() {
	}

	public RepositoryRestApi getRestService() {
		return restService;
	}

	private synchronized static String getRepositoryUrl() {

		if (repositoryUrlLoader == null) {

			Path configFile = Paths.get(
					StandardSystemProperty.USER_HOME.value(), ".kurento",
					"config.properties");

			repositoryUrlLoader = new RepositoryUrlLoader(configFile);
		}

		return repositoryUrlLoader.getRepositoryUrl();
	}
}
