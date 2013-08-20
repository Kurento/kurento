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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kms.api.MediaObjectId;
import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MediaServerService.AsyncClient.connect_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.disconnect_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.getConnectedSinks_call;

/**
 * MediaSrc sends media to one of more MediaSink if linked
 * 
 */
public class MediaSrc extends MediaPad {

	private static final long serialVersionUID = 1L;

	MediaSrc(MediaObjectId mediaSrcId) {
		super(mediaSrcId);
	}

	/* SYNC */

	/**
	 * Creates a link between this object and the given sink
	 * 
	 * @param sink
	 *            The MediaSink that will accept this object media
	 * @throws MediaException
	 * @throws IOException
	 */
	public void connect(MediaSink sink) throws IOException {
		MediaServerService.Client service = mssm.getMediaServerService();

		try {
			service.connect(mediaObjectId, sink.mediaObjectId);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			mssm.releaseMediaServerService(service);
		}
	}

	/**
	 * Unlinks this element and sink
	 * 
	 * @param sink
	 *            The MediaSink that will stop receiving media from this object
	 * @throws MediaException
	 */
	public void disconnect(MediaSink sink) throws IOException {
		MediaServerService.Client service = mssm.getMediaServerService();

		try {
			service.disconnect(mediaObjectId, sink.mediaObjectId);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			mssm.releaseMediaServerService(service);
		}
	}

	public Collection<MediaSink> getConnectedSinks() throws IOException {
		MediaServerService.Client service = mssm.getMediaServerService();

		try {
			List<MediaObjectId> tMediaSinks = service
					.getConnectedSinks(mediaObjectId);
			List<MediaSink> mediaSinks = new ArrayList<MediaSink>();
			for (MediaObjectId tms : tMediaSinks) {
				mediaSinks.add(new MediaSink(tms));
			}
			return mediaSinks;
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			mssm.releaseMediaServerService(service);
		}
	}

	/* ASYNC */

	/**
	 * Creates a link between this object and the given sink
	 * 
	 * @param sink
	 *            The MediaSink that will accept this object media
	 * @throws MediaException
	 * @throws IOException
	 */
	public void connect(MediaSink sink, final Continuation<Void> cont)
			throws IOException {
		MediaServerService.AsyncClient service = mssm
				.getMediaServerServiceAsync();

		try {
			service.connect(
					mediaObjectId,
					sink.mediaObjectId,
					new AsyncMethodCallback<MediaServerService.AsyncClient.connect_call>() {
						@Override
						public void onComplete(connect_call response) {
							try {
								response.getResult();
								cont.onSuccess(null);
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
			mssm.releaseMediaServerServiceAsync(service);
		}
	}

	/**
	 * Unlinks this element and sink
	 * 
	 * @param sink
	 *            The MediaSink that will stop receiving media from this object
	 * @throws MediaException
	 */
	public void disconnect(MediaSink sink, final Continuation<Void> cont)
			throws IOException {
		MediaServerService.AsyncClient service = mssm
				.getMediaServerServiceAsync();

		try {
			service.disconnect(
					mediaObjectId,
					sink.mediaObjectId,
					new AsyncMethodCallback<MediaServerService.AsyncClient.disconnect_call>() {
						@Override
						public void onComplete(disconnect_call response) {
							try {
								response.getResult();
								cont.onSuccess(null);
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
			mssm.releaseMediaServerServiceAsync(service);
		}
	}

	public void getConnectedSinks(final Continuation<Collection<MediaSink>> cont)
			throws IOException {
		MediaServerService.AsyncClient service = mssm
				.getMediaServerServiceAsync();

		try {
			service.getConnectedSinks(
					mediaObjectId,
					new AsyncMethodCallback<MediaServerService.AsyncClient.getConnectedSinks_call>() {
						@Override
						public void onComplete(getConnectedSinks_call response) {
							try {
								List<MediaObjectId> tMediaSinks = response
										.getResult();
								List<MediaSink> mediaSinks = new ArrayList<MediaSink>();
								for (MediaObjectId tms : tMediaSinks) {
									mediaSinks.add(new MediaSink(tms));
								}
								cont.onSuccess(mediaSinks);
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
			mssm.releaseMediaServerServiceAsync(service);
		}
	}

}
