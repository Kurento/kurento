package com.kurento.kmf.content.internal;

public interface StreamingProxyListener {
	public void onProxySuccess();

	public void onProxyError(String message);
}
