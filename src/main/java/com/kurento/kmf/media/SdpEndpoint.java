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
package com.kurento.kmf.media;

/**
 * Implements an SDP negotiation endpoint able to generate and process
 * offers/responses and that configures resources according to negotiated
 * Session Description
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public interface SdpEndpoint extends SessionEndpoint {

	/**
	 * Request a SessionSpec offer.
	 * <p>
	 * This can be used to initiate a connection.
	 * </p>
	 * 
	 * @return The SDP offer.
	 */
	String generateOffer();

	/**
	 * Request the NetworkConnection to process the given SessionSpec offer
	 * (from the remote User Agent).<br>
	 * 
	 * @param offer
	 *            SessionSpec offer from the remote User Agent
	 * @return The chosen configuration from the ones stated in the SDP offer
	 */
	String processOffer(String offer);

	/**
	 * Request the NetworkConnection to process the given SessionSpec answer
	 * (from the remote User Agent).<br>
	 * 
	 * @param answer
	 *            SessionSpec answer from the remote User Agent
	 * @return Updated SDP offer, based on the answer received.
	 */
	String processAnswer(String answer);

	/**
	 * This method gives access to the SessionSpec offered by this
	 * NetworkConnection
	 * 
	 * <p>
	 * <b>Note:</b> This method returns the local MediaSpec, negotiated or not.
	 * If no offer has been generated yet, it returns null. It an offer has been
	 * generated it returns the offer and if an answer has been processed it
	 * returns the negotiated local SessionSpec.
	 * </p>
	 * 
	 * @return The last agreed SessionSpec
	 */
	String getLocalSessionDescriptor();

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
	String getRemoteSessionDescriptor();

	/* ASYNC */

	/**
	 * Request a SessionSpec offer.
	 * <p>
	 * This can be used to initiate a connection.
	 * </p>
	 * 
	 * @param cont
	 *            Continuation object to notify when operation completes
	 */
	void generateOffer(final Continuation<String> cont);

	/**
	 * Request the NetworkConnection to process the given SessionSpec offer
	 * (from the remote User Agent).<br>
	 * 
	 * @param offer
	 *            SessionSpec offer from the remote User Agent
	 * @param cont
	 *            Continuation object to notify when operation completes and to
	 *            provide the answer SessionSpec.
	 */
	void processOffer(String offer, Continuation<String> cont);

	/**
	 * Request the NetworkConnection to process the given SessionSpec answer
	 * (from the remote User Agent).<br>
	 * 
	 * @param answer
	 *            SessionSpec answer from the remote User Agent
	 * @param cont
	 *            Continuation object to notify when operation completes,
	 *            returned SessionSpec is the local SessionSpec.
	 */
	void processAnswer(String answer, Continuation<String> cont);

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
	 * @param cont
	 *            The last agreed SessionSpec
	 * 
	 */
	void getLocalSessionDescriptor(Continuation<String> cont);

	/**
	 * This method gives access to the remote session description.
	 * 
	 * <p>
	 * <b>Note:</b> This method returns the media previously agreed after a
	 * complete offer-answer exchange. If no media has been agreed yet, it
	 * returns null.
	 * </p>
	 * 
	 * @param cont
	 *            The last agreed User Agent session description
	 */
	void getRemoteSessionDescriptor(Continuation<String> cont);

}
