/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package com.kurento.kmf.media;

import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.Param;

/**
 * 
 * Implements an SDP negotiation endpoint able to generate and process
 * offers/responses and that configures resources according to negotiated
 * Session Description
 * 
 **/
@RemoteClass
public interface SdpEndpoint extends SessionEndpoint {

	/**
	 * 
	 * Request a SessionSpec offer. This can be used to initiate a connection.
	 * 
	 * @return The SDP offer. *
	 **/
	String generateOffer();

	/**
	 * 
	 * Asynchronous version of generateOffer: {@link Continuation#onSuccess} is
	 * called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see SdpEndpoint#generateOffer
	 * 
	 **/
	void generateOffer(Continuation<String> cont);

	/**
	 * 
	 * Request the NetworkConnection to process the given SessionSpec offer
	 * (from the remote User Agent)
	 * 
	 * @param offer
	 *            SessionSpec offer from the remote User Agent
	 * @return The chosen configuration from the ones stated in the SDP offer *
	 **/
	String processOffer(@Param("offer") String offer);

	/**
	 * 
	 * Asynchronous version of processOffer: {@link Continuation#onSuccess} is
	 * called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see SdpEndpoint#processOffer
	 * 
	 * @param offer
	 *            SessionSpec offer from the remote User Agent
	 * 
	 **/
	void processOffer(@Param("offer") String offer, Continuation<String> cont);

	/**
	 * 
	 * Request the NetworkConnection to process the given SessionSpec answer
	 * (from the remote User Agent).
	 * 
	 * @param answer
	 *            SessionSpec answer from the remote User Agent
	 * @return Updated SDP offer, based on the answer received. *
	 **/
	String processAnswer(@Param("answer") String answer);

	/**
	 * 
	 * Asynchronous version of processAnswer: {@link Continuation#onSuccess} is
	 * called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see SdpEndpoint#processAnswer
	 * 
	 * @param answer
	 *            SessionSpec answer from the remote User Agent
	 * 
	 **/
	void processAnswer(@Param("answer") String answer, Continuation<String> cont);

	/**
	 * 
	 * This method gives access to the SessionSpec offered by this
	 * NetworkConnection.
	 * <hr/>
	 * <b>Note</b> This method returns the local MediaSpec, negotiated or not.
	 * If no offer has been generated yet, it returns null. It an offer has been
	 * generated it returns the offer and if an answer has been processed it
	 * returns the negotiated local SessionSpec.
	 * 
	 * @return The last agreed SessionSpec *
	 **/
	String getLocalSessionDescriptor();

	/**
	 * 
	 * Asynchronous version of getLocalSessionDescriptor:
	 * {@link Continuation#onSuccess} is called when the action is done. If an
	 * error occurs, {@link Continuation#onError} is called.
	 * 
	 * @see SdpEndpoint#getLocalSessionDescriptor
	 * 
	 **/
	void getLocalSessionDescriptor(Continuation<String> cont);

	/**
	 * 
	 * This method gives access to the remote session description.
	 * <hr/>
	 * <b>Note</b> This method returns the media previously agreed after a
	 * complete offer-answer exchange. If no media has been agreed yet, it
	 * returns null.
	 * 
	 * @return The last agreed User Agent session description *
	 **/
	String getRemoteSessionDescriptor();

	/**
	 * 
	 * Asynchronous version of getRemoteSessionDescriptor:
	 * {@link Continuation#onSuccess} is called when the action is done. If an
	 * error occurs, {@link Continuation#onError} is called.
	 * 
	 * @see SdpEndpoint#getRemoteSessionDescriptor
	 * 
	 **/
	void getRemoteSessionDescriptor(Continuation<String> cont);

}
