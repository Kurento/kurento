package com.kurento.kmf.content.internal;

interface RejectableRunnable extends Runnable {
	void reject(int statusCode, String message);
}
