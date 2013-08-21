package com.kurento.kmf.content.internal;

public interface RejectableRunnable extends Runnable {
	void onExecutionRejected();
}
