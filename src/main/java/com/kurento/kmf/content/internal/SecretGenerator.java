package com.kurento.kmf.content.internal;

import java.math.BigInteger;
import java.security.SecureRandom;

public class SecretGenerator {
	private static SecureRandom secureRandom = new SecureRandom();

	public String nextSecret() {
		// SecureRandom is thread safe, so no synchronization issues here 10
		return new BigInteger(130, secureRandom).toString(32);
	}
}