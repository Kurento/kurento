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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import retrofit.RestAdapter;

public class RestServiceProvider {

	private static final Logger log = LoggerFactory
			.getLogger(RestServiceProvider.class);

	private RepositoryRestApi restService;

	public static RestServiceProvider create(String repoRestUrl) {
		RestServiceProvider provider = new RestServiceProvider();
		RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint(
				repoRestUrl).build();
		provider.restService = restAdapter
				.create(RepositoryRestApi.class);
		log.info("Rest client service created for {}", repoRestUrl);
		return provider;
	}

	RestServiceProvider() {
	}

	public RepositoryRestApi getRestService() {
		return restService;
	}
}
