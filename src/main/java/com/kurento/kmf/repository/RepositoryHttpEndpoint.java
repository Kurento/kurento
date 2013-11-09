package com.kurento.kmf.repository;

import java.io.InputStream;
import java.io.OutputStream;

public interface RepositoryHttpEndpoint {

	/**
	 * Returns the URL to download or upload content for the
	 * {@link RepositoryItem}. When the first client connect to this URL, the
	 * {@link HttpSessionStartedEvent} will be fired to registered listeners.
	 * 
	 * @return
	 */
	String getURL();

	/**
	 * Returns the URL to download or upload content for the
	 * {@link RepositoryItem}. When the first client connect to this URL, the
	 * {@link HttpSessionStartedEvent} will be fired to registered listeners.
	 * 
	 * @return
	 */
	String getDispatchURL();

	/**
	 * Adds the {@link RepoItemHttpEventListener<HttpSessionStartedEvent>} to
	 * this {@link RepositoryHttpEndpoint}. When the media is requested, a
	 * {@link HttpSessionStartedEvent} will be fired to all registered listeners
	 * and the method
	 * {@link RepositoryHttpEventListener#onEvent(RepoItemHttpSessionEvent)}
	 * will be invoked.
	 * 
	 * @param listener
	 */
	void addSessionStartedListener(
			final RepositoryHttpEventListener<HttpSessionStartedEvent> listener);

	/**
	 * Adds the {@link RepoItemHttpEventListener<HttpSessionTerminatedEvent>} to
	 * this {@link RepositoryHttpEndpoint}. When the
	 * {@link RepositoryHttpEndpoint#stop()} method is invoked or a configurable
	 * inactivity time is reached (configurable using
	 * {@link #setAutoTerminationTimeout(long)}) a
	 * {@link HttpSessionTerminatedEventEvent} will be fired to all registered
	 * listeners and the method
	 * {@link RepositoryHttpEventListener#onEvent(RepoItemHttpSessionEvent)}
	 * will be invoked.
	 * 
	 * @param listener
	 */
	void addSessionTerminatedListener(
			final RepositoryHttpEventListener<HttpSessionTerminatedEvent> listener);

	/**
	 * Adds the {@link RepoItemHttpEventListener<HttpSessionErrorEvent>} to this
	 * {@link RepositoryHttpEndpoint}. When an error is produced reading or
	 * writing the media from/to repository a {@link HttpSessionErrorEvent} will
	 * be fired to all registered listeners and the method
	 * {@link RepositoryHttpEventListener#onEvent(RepoItemHttpSessionEvent)}
	 * will be invoked.
	 * 
	 * @param listener
	 */
	void addSessionErrorListener(
			RepositoryHttpEventListener<HttpSessionErrorEvent> listener);

	/**
	 * This method is called to finish the current session. The URL become
	 * invalid to download or upload content and the
	 * {@link HttpSessionTerminatedEvent} is fired to registered listeners.
	 */
	void stop();

	/**
	 * Returns the associated repository item of this
	 * {@link RepositoryHttpEndpoint}
	 * 
	 * @return
	 */
	RepositoryItem getRepositoryItem();

	/**
	 * Returns a new {@link InputStream} on each invocation. It is legal to read
	 * from multiple threads to the same repositoryItem. The returned
	 * {@link InputStream} fully supports skip. The receiver of the
	 * {@link InputStream} is responsible for closing it after its use.
	 * 
	 * @return
	 */
	InputStream createRepoItemInputStream();

	/**
	 * Returns the {@link OutputStream} associated with this
	 * {@link RepositoryHttpEndpoint}. The first time, the {@link OutputStream}
	 * is created and the next times the same {@link OutputStream} is returned.
	 * The returned {@link OutputStream} is not designed to be used concurrently
	 * from several threads. The {@link OutputStream} is closed when the
	 * {@link RepositoryHttpEndpoint#stop()} is invoked or the timeout
	 * {@link RepositoryHttpEndpoint#getAutoTerminationTimeout()} is reached.
	 * 
	 * @return
	 */
	OutputStream getRepoItemOutputStream();

	/**
	 * Sets the time of inactivity to auto-terminate this element. The timer is
	 * started when finish the last request to download or upload the media.
	 */
	void setAutoTerminationTimeout(long timeoutInMillis);

	/**
	 * Gets the current auto-termination timeout
	 * 
	 * @see #setAutoTerminationTimeout(long)
	 * @return the timeout in millis
	 */
	long getAutoTerminationTimeout();

}
