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
package com.kurento.kmf.media.internal.pool;

import static org.apache.commons.pool.impl.GenericObjectPool.WHEN_EXHAUSTED_FAIL;

import java.util.NoSuchElementException;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.MediaApiConfiguration;

public abstract class AbstractPool<T> implements Pool<T> {

	@Autowired
	private MediaApiConfiguration apiConfig;

	private ObjectPool<T> pool;

	protected void init(BasePoolableObjectFactory<T> factory) {
		Config config = new Config();
		config.maxActive = apiConfig.getClientPoolSize();
		config.whenExhaustedAction = WHEN_EXHAUSTED_FAIL;
		this.pool = new GenericObjectPool<T>(factory, config);
	}

	@Override
	public T acquire() throws PoolLimitException,
			KurentoMediaFrameworkException {
		try {
			return this.pool.borrowObject();
		} catch (NoSuchElementException e) {
			throw new PoolLimitException(
					"Max number of pooled sync client instances reached");
		} catch (IllegalStateException e) {
			throw new KurentoMediaFrameworkException(
					"Trying to acquire an object from a closed pool", e, 30000);
		} catch (Exception e) {
			if (e instanceof KurentoMediaFrameworkException) {
				throw (KurentoMediaFrameworkException) e;
			}

			throw new KurentoMediaFrameworkException("Object creation failed",
					e, 30000);
		}
	}

	@Override
	public void release(T obj) {
		try {
			this.pool.returnObject(obj);
		} catch (Exception e) {
			throw new KurentoMediaFrameworkException(
					"Object could not be realeased", e, 30000);
		}
	}
}
