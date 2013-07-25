/*
 * Kurento Commons MSControl: Simplified Media Control API for the Java Platform based on jsr309
 * Copyright (C) 2011  Tikal Technologies
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
import java.security.Policy.Parameters;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kmf.media.internal.MediaServerServiceManager;
import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MediaServerService.AsyncClient.generateOffer_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.getLocalSessionDescription_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.getRemoteSessionDescription_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.processAnswer_call;
import com.kurento.kms.api.MediaServerService.AsyncClient.processOffer_call;
import com.kurento.kms.api.NegotiationException;

/**
 * A NetworkConnection is a {@link MediaElement} that drives network media
 * ports.<br>
 * <p>
 * A NetworkConnection can be created with
 * {@link MediaSession#createNetworkConnection(Parameters)}<br>
 * Example:<br>
 * <code>NetworkConnection myNC = myMediaSession.createNetworkConnection();</code>
 * 
 * It handles a set of media ports, defined by a {@link SessionSpec}.
 * <p>
 * A NetworkConnection can handle multiple streams (audio, video), each of them
 * described by an {@link MediaSession} description.
 * <p>
 * 
 * <pre>
 *  --------------------------------------------------------------------------------
 *  -                                           stream(media description) <-rtp--->-
 *  -   NetworkConnection(session description)  stream(media description) <-rtp--->-
 *  -                                           ....                               -
 *  --------------------------------------------------------------------------------
 * </pre>
 * 
 * Actually two session descriptions are needed:
 * <ul>
 * <li>A <b>Media</b> Server session description describes the mediaserver-local
 * side of a media connection (what the media server accepts to receive)
 * <li>A <b>User Agent</b> session description, describing the remote side (for
 * example a SIPPhone)
 * 
 * 
 * <pre>
 * UserAgent                             Media Server
 *    ----                                 ---------
 *    -  -                                 -       -
 *    -  -        <-------RTP------>       -       -
 *    ----                                 ---------
 * </pre>
 * 
 * The NetworkConnection is compatible with the offer/answer model.
 * <p>
 * The Relationship with SIP signaling messages is described below:
 * 
 * <pre>
 *  A) incoming INVITE with SDP offer:
 *   UserAgent                        Application             NetworkConnection
 *   =============================================================================
 *       ------INVITE----------------->
 *          +userAgentSDP
 *                                     ---------------------->processSdpOffer(userAgentSDP)
 *                                                                     ...................>(media server)
 *                                                                     <...................
 *                                     <-------- Event ----------------
 *                                         +getMediaServerSdp()
 *       <-------- 200 OK -------------
 *              +mediaServerSDP
 * 
 *       --------- ACK -------------->
 * 
 * 
 *  B) incoming INVITE without SDP:
 *   UserAgent                        Application             NetworkConnection
 *   =============================================================================
 *       ------INVITE----------------->
 *                                     ---------------------->generateSDPOffer()
 *                                                                     ...................>(media server)
 *                                                                     <...................
 *                                     <-------- Event ----------------
 *                                         +getMediaServerSdp()
 *       <-------- 200 OK -------------
 *              +mediaServerSDP
 * 
 *       --------- ACK -------------->
 *              +userAgentSDP
 *                                    ------------------------>processSdpAnswer(userAgentSDP)
 * 
 * 
 *  C) outgoing INVITE with SDP offer
 *   UserAgent                        Application             NetworkConnection
 *   =============================================================================
 *                                     ---------------------->generateSDPOffer()
 *                                                                     ...................>(media server)
 *                                                                     <...................
 *                                     <-------- Event ----------------
 *                                         +getMediaServerSdp()
 *       <------INVITE-----------------
 *          +mediaServerSDP
 * 
 *       --------- 200 OK ------------>
 *              +userAgentSDP
 *                                    ------------------------>processSdpAnswer(userAgentSDP)
 *                                                                     ...................>(media server)
 *                                                                     <...................
 *                                     <-------- Event ----------------
 *       <--------- ACK --------------
 *  
 *  D) outgoing INVITE without SDP
 *   UserAgent                        Application             NetworkConnection
 *   =============================================================================
 *       <------INVITE-----------------
 * 
 *       --------- 200 OK ------------>
 *              +userAgentSDP
 *                                     ---------------------->processSdpOffer(userAgentSDP)
 *                                                                     ...................>(media server)
 *                                                                     <...................
 *                                     <-------- Event ----------------
 *                                         +getMediaServerSdp()
 *       <--------- ACK --------------
 *            +mediaServerSDP
 * </pre>
 * 
 * <p>
 * (this is provided as a help in understanding, but the NetworkConnections has
 * no dependency on any signaling protocol, including SIP)
 * </p>
 * </ul>
 */
public abstract class SdpEndPoint extends EndPoint {

	private static final long serialVersionUID = 1L;

	SdpEndPoint(com.kurento.kms.api.MediaObject endpoint) {
		super(endpoint);
	}

	/* SYNC */

	public String generateOffer() throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			String sessionDescriptor = service.generateOffer(mediaObject);
			manager.releaseMediaServerService(service);
			return sessionDescriptor;
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public String processOffer(String offer) throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			String sessionDescriptor = service.processOffer(mediaObject, offer);
			manager.releaseMediaServerService(service);
			return sessionDescriptor;
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public String processAnswer(String answer) throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			String sessionDescriptor = service.processAnswer(mediaObject,
					answer);
			manager.releaseMediaServerService(service);
			return sessionDescriptor;
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	/**
	 * This method gives access to the SessionSpec offered by this
	 * NetworkConnection
	 * 
	 * <p>
	 * <b>Note:</b> This method returns the local MediaSpec, negotiated or not.
	 * If no offer has been generated yet, it returns null. It an offer has been
	 * generated it returns the offer and if an asnwer has been processed it
	 * returns the negotiated local SessionSpec.
	 * </p>
	 * 
	 * @return The last agreed SessionSpec
	 * @throws IOException
	 */
	public String getLocalSessionDescriptor() throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			String sessionDescriptor = service
					.getLocalSessionDescription(mediaObject);
			manager.releaseMediaServerService(service);
			return sessionDescriptor;
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	/**
	 * This method gives access to the remote session description.
	 * 
	 * <p>
	 * <b>Note:</b> This method returns the media previously agreed after a
	 * complete offer-answer exchange. If no media has been agreed yet, it
	 * returns null.
	 * </p>
	 * 
	 * @return The last agreed User Agent session description
	 */
	public String getRemoteSessionDescriptor() throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			String sessionDescriptor = service
					.getRemoteSessionDescription(mediaObject);
			manager.releaseMediaServerService(service);
			return sessionDescriptor;
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	/* ASYNC */

	/**
	 * Request a SessionSpec offer.
	 * 
	 * <p>
	 * The resulting offer is available with
	 * {@link SdpEndPoint#getSessionSpec()}
	 * </p>
	 * 
	 * <p>
	 * This can be used to initiate a connection.
	 * </p>
	 * 
	 * @param cont
	 *            Continuation object to notify when operation completes
	 * @throws MediaException
	 */
	public void generateOffer(final Continuation<String> cont)
			throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.AsyncClient service = manager
					.getMediaServerServiceAsync();
			service.generateOffer(
					mediaObject,
					new AsyncMethodCallback<MediaServerService.AsyncClient.generateOffer_call>() {
						@Override
						public void onComplete(generateOffer_call response) {
							try {
								String sessionDescriptor = response.getResult();
								cont.onSuccess(sessionDescriptor);
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
			manager.releaseMediaServerServiceAsync(service);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	/**
	 * Request the NetworkConnection to process the given SessionSpec offer
	 * (from the remote User Agent).<br>
	 * The resulting answer is available with
	 * {@link SdpEndPoint#getSessionSpec()} and the remote offer will be
	 * returned by {@link SdpEndPoint#getRemoteSessionSpec()}
	 * 
	 * @param offer
	 *            SessionSpec offer from the remote User Agent
	 * @param cont
	 *            Continuation object to notify when operation completes and to
	 *            provide the answer SessionSpec.
	 */
	public void processOffer(String offer, final Continuation<String> cont)
			throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.AsyncClient service = manager
					.getMediaServerServiceAsync();
			service.processOffer(
					mediaObject,
					offer,
					new AsyncMethodCallback<MediaServerService.AsyncClient.processOffer_call>() {
						@Override
						public void onComplete(processOffer_call response) {
							try {
								String sessionDescriptor = response.getResult();
								cont.onSuccess(sessionDescriptor);
							} catch (MediaObjectNotFoundException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (NegotiationException e) {
								cont.onError(new MediaException(e.getMessage(),
										e));
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
			manager.releaseMediaServerServiceAsync(service);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	/**
	 * Request the NetworkConnection to process the given SessionSpec answer
	 * (from the remote User Agent).<br>
	 * The answer become available on method
	 * {@link SdpEndPoint#getRemoteSessionSpec()}
	 * 
	 * @param answer
	 *            SessionSpec answer from the remote User Agent
	 * @param cont
	 *            Continuation object to notify when operation completes,
	 *            returned SessionSpec is the local SessionSpec.
	 */
	public void processAnswer(String answer, final Continuation<String> cont)
			throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.AsyncClient service = manager
					.getMediaServerServiceAsync();
			service.processAnswer(
					mediaObject,
					answer,
					new AsyncMethodCallback<MediaServerService.AsyncClient.processAnswer_call>() {
						@Override
						public void onComplete(processAnswer_call response) {
							try {
								String sessionDescriptor = response.getResult();
								cont.onSuccess(sessionDescriptor);
							} catch (MediaObjectNotFoundException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (NegotiationException e) {
								cont.onError(new MediaException(e.getMessage(),
										e));
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
		}
	}

	/**
	 * This method gives access to the SessionSpec offered by this
	 * NetworkConnection
	 * 
	 * <p>
	 * <b>Note:</b> This method returns the local MediaSpec, negotiated or not.
	 * If no offer has been generated yet, it returns null. It an offer has been
	 * generated it returns the offer and if an asnwer has been processed it
	 * returns the negotiated local SessionSpec.
	 * </p>
	 * 
	 * @return The last agreed SessionSpec
	 * @throws IOException
	 */
	public void getLocalSessionDescriptor(final Continuation<String> cont)
			throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.AsyncClient service = manager
					.getMediaServerServiceAsync();
			service.getLocalSessionDescription(
					mediaObject,
					new AsyncMethodCallback<MediaServerService.AsyncClient.getLocalSessionDescription_call>() {
						@Override
						public void onComplete(
								getLocalSessionDescription_call response) {
							try {
								String sessionDescriptor = response.getResult();
								cont.onSuccess(sessionDescriptor);
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
			manager.releaseMediaServerServiceAsync(service);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	/**
	 * This method gives access to the remote session description.
	 * 
	 * <p>
	 * <b>Note:</b> This method returns the media previously agreed after a
	 * complete offer-answer exchange. If no media has been agreed yet, it
	 * returns null.
	 * </p>
	 * 
	 * @return The last agreed User Agent session description
	 */
	public void getRemoteSessionDescriptor(final Continuation<String> cont)
			throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.AsyncClient service = manager
					.getMediaServerServiceAsync();
			service.getRemoteSessionDescription(
					mediaObject,
					new AsyncMethodCallback<MediaServerService.AsyncClient.getRemoteSessionDescription_call>() {
						@Override
						public void onComplete(
								getRemoteSessionDescription_call response) {
							try {
								String sessionDescriptor = response.getResult();
								cont.onSuccess(sessionDescriptor);
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
			manager.releaseMediaServerServiceAsync(service);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

}
