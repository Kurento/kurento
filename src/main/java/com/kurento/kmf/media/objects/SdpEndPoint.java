package com.kurento.kmf.media.objects;

import java.io.IOException;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.commands.MediaCommand;
import com.kurento.kmf.media.commands.StringCommandResult;
import com.kurento.kmf.media.internal.StringCommand;
import com.kurento.kmf.media.internal.StringContinuationWrapper;
import com.kurento.kmf.media.internal.VoidCommand;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

// TODO: update doc
/**
 * A NetworkConnection is a {@link zMediaElement} that drives network media
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

	SdpEndPoint(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

	/* SYNC */
	public String generateOffer() {
		MediaCommand command = new VoidCommand("generateOffer");
		StringCommandResult result = (StringCommandResult) sendCommand(command);
		return result.getString();
	}

	public String processOffer(String offer) {
		MediaCommand command = new StringCommand("processOffer");
		StringCommandResult result = (StringCommandResult) sendCommand(command);
		return result.getString();

	}

	public String processAnswer(String answer) {
		MediaCommand command = new VoidCommand("processAnswer");
		StringCommandResult result = (StringCommandResult) sendCommand(command);
		return result.getString();
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
	public String getLocalSessionDescriptor() {
		MediaCommand command = new VoidCommand("getLocalSessionDescriptor");
		StringCommandResult result = (StringCommandResult) sendCommand(command);
		return result.getString();
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
	public String getRemoteSessionDescriptor() {
		MediaCommand command = new VoidCommand("getRemoteSessionDescriptor");
		StringCommandResult result = (StringCommandResult) sendCommand(command);
		return result.getString();
	}

	/* ASYNC */

	/**
	 * Request a SessionSpec offer.
	 * 
	 * <p>
	 * The resulting offer is available with
	 * {@link zSdpEndPoint#getSessionSpec()}
	 * </p>
	 * 
	 * <p>
	 * This can be used to initiate a connection.
	 * </p>
	 * 
	 * @param cont
	 *            Continuation object to notify when operation completes
	 * @throws zMediaException
	 */
	public void generateOffer(final Continuation<String> cont) {
		MediaCommand command = new VoidCommand("generateOffer");
		StringContinuationWrapper wrappedCont = new StringContinuationWrapper(
				cont);
		sendCommand(command, wrappedCont);
	}

	/**
	 * Request the NetworkConnection to process the given SessionSpec offer
	 * (from the remote User Agent).<br>
	 * The resulting answer is available with
	 * {@link zSdpEndPoint#getSessionSpec()} and the remote offer will be
	 * returned by {@link zSdpEndPoint#getRemoteSessionSpec()}
	 * 
	 * @param offer
	 *            SessionSpec offer from the remote User Agent
	 * @param cont
	 *            Continuation object to notify when operation completes and to
	 *            provide the answer SessionSpec.
	 */
	public void processOffer(String offer, final Continuation<String> cont) {
		MediaCommand command = new VoidCommand("processOffer");
		StringContinuationWrapper wrappedCont = new StringContinuationWrapper(
				cont);
		sendCommand(command, wrappedCont);
	}

	/**
	 * Request the NetworkConnection to process the given SessionSpec answer
	 * (from the remote User Agent).<br>
	 * The answer become available on method
	 * {@link zSdpEndPoint#getRemoteSessionSpec()}
	 * 
	 * @param answer
	 *            SessionSpec answer from the remote User Agent
	 * @param cont
	 *            Continuation object to notify when operation completes,
	 *            returned SessionSpec is the local SessionSpec.
	 */
	public void processAnswer(String answer, final Continuation<String> cont) {
		MediaCommand command = new VoidCommand("processAnswer");
		StringContinuationWrapper wrappedCont = new StringContinuationWrapper(
				cont);
		sendCommand(command, wrappedCont);
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
	public void getLocalSessionDescriptor(final Continuation<String> cont) {
		MediaCommand command = new VoidCommand("getLocalSessionDescriptor");
		StringContinuationWrapper wrappedCont = new StringContinuationWrapper(
				cont);
		sendCommand(command, wrappedCont);
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
	public void getRemoteSessionDescriptor(final Continuation<String> cont) {
		MediaCommand command = new VoidCommand("getRemoteSessionDescriptor");
		StringContinuationWrapper wrappedCont = new StringContinuationWrapper(
				cont);
		sendCommand(command, wrappedCont);
	}
}
