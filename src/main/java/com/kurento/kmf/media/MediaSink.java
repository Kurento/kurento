/*
 * Kurento Commons MSControl: Simplified Media Control API for the Java Platform based on jsr309
 * Copyright (C) 2012  Tikal Technologies
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.kurento.kmf.media;

import java.io.IOException;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.transport.TTransportException;

import com.kurento.kmf.media.internal.MediaServerServiceManager;
import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MediaServerService.AsyncClient.getConnectedSrc_call;

/**
 * A MediaSink receives media from a connected MediaSrc (if any)
 * 
 */
public class MediaSink extends MediaPad {

	private static final long serialVersionUID = 1L;

	MediaSink(com.kurento.kms.api.MediaObject mediaSink) {
		super(mediaSink);
	}

	/* SYNC */

	/**
	 * Returns the connected MediaSrc or null if not connected
	 * 
	 * @return The connected MediaSrc or null if not connected
	 * @throws IOException
	 */
	public MediaSrc getConnectedSrc() throws IOException {
		MediaServerServiceManager manager = MediaServerServiceManager
				.getInstance();
		MediaServerService.Client service;
		try {
			service = manager.getMediaServerService();
		} catch (TTransportException e) {
			throw new IOException(e.getMessage(), e);
		}

		try {
			com.kurento.kms.api.MediaObject connectedSrc = service
					.getConnectedSrc(mediaObject);
			if (connectedSrc == null)
				return null;
			return new MediaSrc(connectedSrc);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			manager.releaseMediaServerService(service);
		}
	}

	/* ASYNC */

	/**
	 * Returns the connected MediaSrc or null if not connected
	 * 
	 * @return The connected MediaSrc or null if not connected
	 * @throws IOException
	 */
	public void getConnectedSrc(final Continuation<MediaSrc> cont)
			throws IOException {
		MediaServerServiceManager manager = MediaServerServiceManager
				.getInstance();
		MediaServerService.AsyncClient service;
		try {
			service = manager.getMediaServerServiceAsync();
		} catch (TTransportException e) {
			throw new IOException(e.getMessage(), e);
		}

		try {
			service.getConnectedSrc(
					mediaObject,
					new AsyncMethodCallback<MediaServerService.AsyncClient.getConnectedSrc_call>() {
						@Override
						public void onComplete(getConnectedSrc_call response) {
							try {
								com.kurento.kms.api.MediaObject connectedSrc = response
										.getResult();
								if (connectedSrc == null) {
									cont.onSuccess(null);
								} else {
									cont.onSuccess(new MediaSrc(connectedSrc));
								}
							} catch (MediaObjectNotFoundException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (MediaServerException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (TException e) {
								cont.onError(new IOException(e.getMessage(), e));
							}
						}

						@Override
						public void onError(Exception exception) {
							cont.onError(exception);
						}
					});
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			manager.releaseMediaServerServiceAsync(service);
		}
	}

}
