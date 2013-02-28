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

package com.kurento.kms.media;

import java.io.IOException;

import org.apache.thrift.TException;

import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MediaType;
import com.kurento.kms.media.internal.MediaServerServiceManager;

/**
 * A MediaSink receives media from a connected MediaSrc (if any)
 * 
 */
public class MediaSink extends MediaObject {

	public MediaSink(com.kurento.kms.api.MediaObject mediaSink) {
		super(mediaSink);
	}

	/**
	 * Returns the Joined MediaSrc or null if not joined
	 * 
	 * @return The joined MediaSrc or null if not joined
	 * @throws IOException
	 */
	public MediaSrc getConnectedSrc() throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			com.kurento.kms.api.MediaObject connectedSrc = service
					.getConnectedSrc(mediaObject);
			manager.releaseMediaServerService(service);
			return new MediaSrc(connectedSrc);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public MediaType getMediaType() throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			MediaType mediaType = service.getMediaType(mediaObject);
			manager.releaseMediaServerService(service);
			return mediaType;
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}
}
