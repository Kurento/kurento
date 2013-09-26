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
package com.kurento.kmf.common;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * 
 * Random word (integer) generator.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class SecretGenerator {

	/**
	 * Secure random generator object.
	 */
	private static SecureRandom secureRandom = new SecureRandom();

	/**
	 * Random word generator.
	 * 
	 * @return Generated word
	 */
	public String nextSecret() {
		// SecureRandom is thread safe, so no synchronization issues here 10
		return new BigInteger(130, secureRandom).toString(32);
	}
}
