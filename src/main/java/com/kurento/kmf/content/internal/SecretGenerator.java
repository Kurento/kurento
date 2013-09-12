package com.kurento.kmf.content.internal;

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
