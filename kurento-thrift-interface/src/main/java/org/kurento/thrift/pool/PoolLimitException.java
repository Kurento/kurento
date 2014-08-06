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

import org.kurento.thrift.ThriftInterfaceException;

/**
 * This exception is used when the pool is exhausted and no more clients can be
 * created.
 * 
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 1.0.0
 *
 */
public class PoolLimitException extends ThriftInterfaceException {

	private static final long serialVersionUID = 1428726405659800781L;

	public PoolLimitException(String message) {
		super(message);
	}

	public PoolLimitException(String message, Throwable cause) {
		super(message, cause);
	}
}
