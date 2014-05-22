package com.kurento.kmf.phone;

import com.kurento.kmf.jsonrpcconnector.Session;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.WebRtcEndpoint;
import com.kurento.kmf.media.factory.MediaPipelineFactory;

public class Call {

	private MediaPipelineFactory mpf;
	private MediaPipeline mp;
	
	private String ipName;
	private Session ipSession;
	private WebRtcEndpoint ipWebRtcEP;	
	
	private String opName;
	private Session opSession;
	private WebRtcEndpoint opWebRtcEP;
		
	public Call(MediaPipelineFactory mpf) {
		this.mpf = mpf;
		this.mp = mpf.create();
		this.ipWebRtcEP = mp.newWebRtcEndpoint().build();
		this.opWebRtcEP = mp.newWebRtcEndpoint().build();
		
		this.ipWebRtcEP.connect(this.opWebRtcEP);
		this.opWebRtcEP.connect(this.ipWebRtcEP);
	}

	public WebRtcEndpoint getWebRtcForIncommingPeer() {				
		return ipWebRtcEP;
	}

	public WebRtcEndpoint getWebRtcForOutgoingPeer() {				
		return opWebRtcEP;
	}

	public void setOutgoingPeer(String opName, Session opSession) {
		this.opName = opName;
		this.opSession = opSession;
	}

	public void setIncommingPeer(String ipName, Session ipSession) {
		this.ipName = ipName;
		this.ipSession = ipSession;		
	}
}
