/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

package org.kurento.kmf.repository;

import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;

/**
 * An object implementing this interface represents an http endpoint to play
 * (download) or record (upload) the contents of a repository item.
 * 
 * This endpoints are created using the methods
 * {@link RepositoryItem#createRepositoryHttpPlayer()} or
 * {@link RepositoryItem#createRepositoryHttpRecorder()} within the repository
 * item that want to be played or recorded.
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 */
public interface RepositoryHttpEndpoint {

	/**
	 * Returns the URL to play (download) or record (upload) the content for the
	 * {@link RepositoryItem}. When the first client connect to this URL, the
	 * {@link HttpSessionStartedEvent} will be fired to registered listeners.
	 * 
	 * @return the URL to play or record
	 */
	String getURL();

	/**
	 * Returns the URL to play (download) or record (upload) the content
	 * for the {@link RepositoryItem}. This URL is relative to the context URL
	 * of this app. The URL can be used to dispatch a request with
	 * {@link javax.servlet.ServletRequest#getRequestDispatcher(java.lang.String)
	 * getRequestDispatcher}. When the first client connect to this URL, the
	 * {@link HttpSessionStartedEvent} will be fired to registered listeners.
	 * 
	 * @return the relative URL to play or record
	 */
	String getDispatchURL();

	/**
	 * Adds the {@link RepositoryHttpEventListener}&lt;{@link
	 * HttpSessionStartedEvent}> to this {@link RepositoryHttpEndpoint}.
	 * When the media is requested, a {@link HttpSessionStartedEvent} will
	 * be fired to all registered listenersand the method {@link
	 * RepositoryHttpEventListener#onEvent(RepositoryHttpSessionEvent)
	 * onEvent} will be invoked.
	 * 
	 * @param listener
	 */
	void addSessionStartedListener(
			final RepositoryHttpEventListener<HttpSessionStartedEvent> listener);

	/**
	 * Adds the {@link RepositoryHttpEventListener}&lt;{@link
	 * HttpSessionTerminatedEvent}> to this {@link RepositoryHttpEndpoint}.
	 * When the {@link RepositoryHttpEndpoint#stop()} method is invoked
	 * or a configurable inactivity time is reached (configurable using
	 * {@link #setAutoTerminationTimeout(long)}) a {@link
	 * HttpSessionTerminatedEvent} will be fired to all registered listeners
	 * and the method {@link
	 * RepositoryHttpEventListener#onEvent(RepositoryHttpSessionEvent) onEvent}
	 * will be invoked.
	 * 
	 * @param listener
	 */
	void addSessionTerminatedListener(
			final RepositoryHttpEventListener<HttpSessionTerminatedEvent> listener);

	/**
	 * Adds the {@link RepositoryHttpEventListener}&lt;{@link HttpSessionErrorEvent}>
	 * to this {@link RepositoryHttpEndpoint}. When an error is produced reading or
	 * writing the media from/to repository a {@link HttpSessionErrorEvent} will
	 * be fired to all registered listeners and the method {@link
	 * RepositoryHttpEventListener#onEvent(RepositoryHttpSessionEvent) onEvent}
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
	 * @return The repository item associated to the endpoint
	 */
	RepositoryItem getRepositoryItem();

	/**
	 * Returns a new {@link InputStream} on each invocation. It is legal to read
	 * from multiple threads to the same repositoryItem. The returned
	 * {@link InputStream} fully supports skip. The receiver of the
	 * {@link InputStream} is responsible for closing it after its use.
	 * 
	 * @return An input stream to read item content
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
	 * @return An output stream to write item content
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
