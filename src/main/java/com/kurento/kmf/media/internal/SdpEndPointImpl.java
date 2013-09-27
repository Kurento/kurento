package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.SdpEndPoint;
import com.kurento.kmf.media.commands.internal.GenerateSdpOfferCommand;
import com.kurento.kmf.media.commands.internal.GetLocalSdpCommand;
import com.kurento.kmf.media.commands.internal.GetRemoteSdpCommand;
import com.kurento.kmf.media.commands.internal.ProcessSdpAnswerCommand;
import com.kurento.kmf.media.commands.internal.ProcessSdpOfferCommand;
import com.kurento.kmf.media.commands.internal.StringCommandResult;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

public abstract class SdpEndPointImpl extends EndPointImpl implements
		SdpEndPoint {

	SdpEndPointImpl(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

	/* SYNC */
	@Override
	public String generateOffer() {
		StringCommandResult result = (StringCommandResult) sendCommand(new GenerateSdpOfferCommand());
		return result.getResult();
	}

	@Override
	public String processOffer(String offer) {
		StringCommandResult result = (StringCommandResult) sendCommand(new ProcessSdpOfferCommand(
				offer));
		return result.getResult();
	}

	@Override
	public String processAnswer(String answer) {
		StringCommandResult result = (StringCommandResult) sendCommand(new ProcessSdpAnswerCommand(
				answer));
		return result.getResult();
	}

	@Override
	public String getLocalSessionDescriptor() {
		StringCommandResult result = (StringCommandResult) sendCommand(new GetLocalSdpCommand());
		return result.getResult();
	}

	@Override
	public String getRemoteSessionDescriptor() {
		StringCommandResult result = (StringCommandResult) sendCommand(new GetRemoteSdpCommand());
		return result.getResult();
	}

	/* ASYNC */

	@Override
	public void generateOffer(final Continuation<String> cont) {
		sendCommand(new GenerateSdpOfferCommand(),
				new StringContinuationWrapper(cont));
	}

	@Override
	public void processOffer(String offer, final Continuation<String> cont) {
		sendCommand(new ProcessSdpOfferCommand(offer),
				new StringContinuationWrapper(cont));
	}

	@Override
	public void processAnswer(String answer, final Continuation<String> cont) {
		sendCommand(new ProcessSdpAnswerCommand(answer),
				new StringContinuationWrapper(cont));
	}

	@Override
	public void getLocalSessionDescriptor(final Continuation<String> cont) {
		sendCommand(new GetLocalSdpCommand(), new StringContinuationWrapper(
				cont));
	}

	@Override
	public void getRemoteSessionDescriptor(final Continuation<String> cont) {
		sendCommand(new GetRemoteSdpCommand(), new StringContinuationWrapper(
				cont));
	}
}
