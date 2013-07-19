package com.kurento.kmf.content.servlet;

interface RejectableRunnable extends Runnable {
	void reject(int statusCode, String message);
}
