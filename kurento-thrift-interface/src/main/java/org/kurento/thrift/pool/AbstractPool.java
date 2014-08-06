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

import java.util.NoSuchElementException;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PoolUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.kurento.commons.exception.KurentoException;
import org.kurento.thrift.ThriftInterfaceConfiguration;

public abstract class AbstractPool<T> implements Pool<T> {

	@Autowired
	private ThriftInterfaceConfiguration apiConfig;

	private ObjectPool<T> pool;

	/**
	 * Default constructor, to be used in spring environments
	 */
	public AbstractPool() {
	}

	/**
	 * Constructor for non-spring environments.
	 * 
	 * @param apiConfig
	 *            configuration object
	 */
	public AbstractPool(ThriftInterfaceConfiguration apiConfig) {
		this.apiConfig = apiConfig;
	}

	protected void init(BasePooledObjectFactory<T> factory) {
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMinIdle(apiConfig.getClientPoolSize());
		config.setTestOnBorrow(true);
		config.setTestOnReturn(true);
		this.pool = PoolUtils.erodingPool(new GenericObjectPool<>(factory,
				config));
	}

	@Override
	public T acquire() throws PoolLimitException, KurentoException,
			ClientPoolException {
		try {
			return this.pool.borrowObject();
		} catch (NoSuchElementException e) {
			throw new PoolLimitException(
					"Max number of pooled client instances reached");
		} catch (IllegalStateException e) {
			throw new ClientPoolException(
					"Trying to acquire an object from a closed pool", e);
		} catch (Exception e) {
			if (e instanceof KurentoException) {
				throw (KurentoException) e;
			}

			throw new ClientPoolException("Object creation failed", e);
		}
	}

	@Override
	public void release(T obj) throws ClientPoolException {
		try {
			this.pool.returnObject(obj);
		} catch (Exception e) {
			throw new ClientPoolException("Object could not be realeased", e);
		}
	}
}
