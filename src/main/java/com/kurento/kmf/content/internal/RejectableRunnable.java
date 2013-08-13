package com.kurento.kmf.content.internal;

interface RejectableRunnable extends Runnable {
	void onExecutionRejected();
}
