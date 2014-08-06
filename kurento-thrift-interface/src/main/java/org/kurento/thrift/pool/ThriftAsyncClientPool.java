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
package org.kurento.thrift.pool;

import javax.annotation.PostConstruct;

import org.kurento.thrift.ThriftInterfaceConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;

public class ThriftAsyncClientPool extends AbstractPool<AsyncClient> {

	@Autowired
	private ThriftAsyncClientFactory asyncFactory;

	/**
	 * Default constructor, to be used in spring environments
	 */
	public ThriftAsyncClientPool() {
	}

	/**
	 * Constructor for non-spring environments.
	 * 
	 * @param asyncFactory
	 *            the factory that builds async clients
	 * @param cfg
	 *            configuration object
	 */
	public ThriftAsyncClientPool(ThriftAsyncClientFactory asyncFactory,
			ThriftInterfaceConfiguration cfg) {
		super(cfg);
		this.asyncFactory = asyncFactory;
		init();
	}

	@PostConstruct
	private void init() {
		super.init(asyncFactory);
	}
}
