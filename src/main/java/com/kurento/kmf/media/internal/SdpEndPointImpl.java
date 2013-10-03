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
package com.kurento.kmf.media.internal;

import static com.kurento.kms.thrift.api.KmsMediaSdpEndPointTypeConstants.GENERATE_SDP_OFFER;
import static com.kurento.kms.thrift.api.KmsMediaSdpEndPointTypeConstants.GET_LOCAL_SDP;
import static com.kurento.kms.thrift.api.KmsMediaSdpEndPointTypeConstants.GET_REMOTE_SDP;
import static com.kurento.kms.thrift.api.KmsMediaSdpEndPointTypeConstants.PROCESS_SDP_ANSWER;
import static com.kurento.kms.thrift.api.KmsMediaSdpEndPointTypeConstants.PROCESS_SDP_OFFER;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.SdpEndPoint;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.params.internal.StringMediaParam;

public abstract class SdpEndPointImpl extends AbstractSessionEndPoint implements
		SdpEndPoint {

	SdpEndPointImpl(MediaElementRef endpointRef) {
		super(endpointRef);
	}

	/* SYNC */
	@Override
	public String generateOffer() {
		StringMediaParam result = (StringMediaParam) invoke(GENERATE_SDP_OFFER);
		return result.getString();
	}

	@Override
	public String processOffer(String offer) {
		// TODO build params map offer
		StringMediaParam result = (StringMediaParam) invoke(PROCESS_SDP_OFFER);
		return result.getString();
	}

	@Override
	public String processAnswer(String answer) {
		// TODO build params map answer
		StringMediaParam result = (StringMediaParam) invoke(PROCESS_SDP_ANSWER);
		return result.getString();
	}

	@Override
	public String getLocalSessionDescriptor() {
		StringMediaParam result = (StringMediaParam) invoke(GET_LOCAL_SDP);
		return result.getString();
	}

	@Override
	public String getRemoteSessionDescriptor() {
		StringMediaParam result = (StringMediaParam) invoke(GET_REMOTE_SDP);
		return result.getString();
	}

	/* ASYNC */

	@Override
	public void generateOffer(final Continuation<String> cont) {
		invoke(GENERATE_SDP_OFFER, new StringContinuationWrapper(cont));
	}

	@Override
	public void processOffer(String offer, final Continuation<String> cont) {
		// TODO build params map offer
		invoke(PROCESS_SDP_OFFER, new StringContinuationWrapper(cont));
	}

	@Override
	public void processAnswer(String answer, final Continuation<String> cont) {
		// TODO build params map answer
		invoke(PROCESS_SDP_ANSWER, new StringContinuationWrapper(cont));
	}

	@Override
	public void getLocalSessionDescriptor(final Continuation<String> cont) {
		invoke(GET_LOCAL_SDP, new StringContinuationWrapper(cont));
	}

	@Override
	public void getRemoteSessionDescriptor(final Continuation<String> cont) {
		invoke(GET_REMOTE_SDP, new StringContinuationWrapper(cont));
	}
}
