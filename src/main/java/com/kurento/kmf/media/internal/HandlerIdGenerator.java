package com.kurento.kmf.media.internal;

import java.util.Random;

public class HandlerIdGenerator {
	private int handlerId;

	// TODO handlerId should be, at least, a long and should be generated in a
	// criptographically strong manner.
	public HandlerIdGenerator() {
		handlerId = new Random(System.nanoTime()).nextInt();
	}

	public int getHandlerId() {
		return handlerId;
	}
}
