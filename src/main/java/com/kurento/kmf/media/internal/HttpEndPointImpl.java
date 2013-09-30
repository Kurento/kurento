package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.HttpEndPoint;
import com.kurento.kmf.media.commands.internal.GetUrlCommand;
import com.kurento.kmf.media.commands.internal.StringCommandResult;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;
import com.kurento.kms.thrift.api.mediaServerConstants;

@ProvidesMediaElement(type = mediaServerConstants.HTTP_END_POINT_TYPE)
public class HttpEndPointImpl extends EndPointImpl implements HttpEndPoint {

	HttpEndPointImpl(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

	/* SYNC */
	@Override
	public String getUrl() {
		StringCommandResult result = (StringCommandResult) sendCommand(new GetUrlCommand());
		return result.getResult();
	}

	/* ASYNC */

	@Override
	public void getUrl(final Continuation<String> cont) {
		sendCommand(new GetUrlCommand(), new StringContinuationWrapper(cont));
	}
}
