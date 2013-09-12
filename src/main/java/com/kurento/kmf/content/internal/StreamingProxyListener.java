package com.kurento.kmf.content.internal;

/**
 * 
 * Streaming proxy triggers events depending on the result of its operation,
 * this interfaces defines these events ( {@link #onProxySuccess()},
 * {@link #onProxyError(String)}).
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
public interface StreamingProxyListener {
	/**
	 * Proxy success event declaration.
	 */
	public void onProxySuccess();

	/**
	 * Proxy error event declaration.
	 */
	public void onProxyError(String message);
}
