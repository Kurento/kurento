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

import java.security.Policy.Parameters;
import com.kurento.mediaspec.SessionSpec;

/**
 * A NetworkConnection is a {@link Joinable} that drives network media ports.<br>
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
public interface Stream extends MediaResource, Joinable {

	/**
	 * Request a SessionSpec offer.
	 * 
	 * <p>
	 * The resulting offer is available with {@link Stream#getSessionSpec()}
	 * </p>
	 * 
	 * <p>
	 * This can be used to initiate a connection.
	 * </p>
	 * 
	 * @param cont
	 *            Continuation object to notify when operation completes
	 */
	void generateOffer(Continuation cont);

	/**
	 * Request the NetworkConnection to process the given SessionSpec offer
	 * (from the remote User Agent).<br>
	 * The resulting answer is available with {@link Stream#getSessionSpec()}
	 * and the remote offer will be returned by
	 * {@link Stream#getRemoteSessionSpec()}
	 * 
	 * @param offer
	 *            SessionSpec offer from the remote User Agent
	 * @param cont
	 *            Continuation object to notify when operation completes and to
	 *            provide the answer SessionSpec.
	 */
	void processOffer(SessionSpec offer, Continuation cont);

	/**
	 * Request the NetworkConnection to process the given SessionSpec answer
	 * (from the remote User Agent).<br>
	 * The answer become available on method
	 * {@link Stream#getRemoteSessionSpec()}
	 * 
	 * @param answer
	 *            SessionSpec answer from the remote User Agent
	 * @param cont
	 *            Continuation object to notify when operation completes,
	 *            returned SessionSpec is the local SessionSpec.
	 */
	void processAnswer(SessionSpec answer, Continuation cont);

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
	 */
	SessionSpec getSessionDescriptor();

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
	SessionSpec getRemoteSessionDescriptor();

	/**
	 * Used as a callback for some asynchronous NetworkConnection actions
	 * 
	 */
	public interface Continuation {

		/**
		 * This method is called when the operation success
		 * 
		 * @param spec
		 *            The generated session spec
		 */
		public void onSucess(SessionSpec spec);

		/**
		 * This method gets called when the operation fails
		 * 
		 * @param cause
		 *            The cause of the failure
		 */
		public void onError(Throwable cause);
	}
}
