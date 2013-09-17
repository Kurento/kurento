package com.kurento.kmf.media.pool;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.exceptions.PoolLimitException;
import com.kurento.kms.thrift.api.MediaServerException;

public interface Pool<T> {

	/**
	 * Retrieves an object from the pool
	 * 
	 * @return An object from the pool
	 * @throws PoolLimitException
	 *             If the maximum number of allowed instances have already been
	 *             created
	 * @throws InvokationException
	 *             If an object can´t be created
	 * @throws MediaServerException
	 */
	public T acquire() throws PoolLimitException,
			KurentoMediaFrameworkException;

	/**
	 * Returns an object to the pool
	 * 
	 * @param obj
	 * @throws MediaServerException
	 *             In case of an internal exception in the pool
	 */
	public void release(T obj) throws KurentoMediaFrameworkException;
}
