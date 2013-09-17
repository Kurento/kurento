package com.kurento.kmf.media.pool;

import static org.apache.commons.pool.impl.GenericObjectPool.WHEN_EXHAUSTED_FAIL;

import java.util.NoSuchElementException;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.exceptions.PoolLimitException;
import com.kurento.kmf.media.internal.MediaApiConfiguration;

abstract class AbstractPool<T> implements Pool<T> {

	@Autowired
	private MediaApiConfiguration apiConfig;

	private final ObjectPool<T> pool;

	AbstractPool(BasePoolableObjectFactory<T> factory) {
		Config config = new Config();
		config.maxActive = this.apiConfig.getPoolSize();
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
			} else {
				throw new KurentoMediaFrameworkException(
						"Object creation failed", e, 30000);
			}
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
