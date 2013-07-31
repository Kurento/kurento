package com.kurento.kmf.content.internal;

import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import com.kurento.kmf.content.WebRtcMediaHandler;
import com.kurento.kmf.spring.KurentoApplicationContextUtils;

public class WebRtcMediaRequestManager {
	private ConcurrentHashMap<String, WebRtcMediaRequestImpl> requests = new ConcurrentHashMap<String, WebRtcMediaRequestImpl>();

	public WebRtcMediaRequestManager() {
	}

	public WebRtcMediaRequestImpl create(WebRtcMediaHandler handler,
			String contentId, HttpServletRequest httpServletRequest) {
		WebRtcMediaRequestImpl request = (WebRtcMediaRequestImpl) KurentoApplicationContextUtils
				.getBean("webRtcMediaRequestImpl", handler, this, contentId,
						httpServletRequest);
		requests.put(request.getSessionId(), request);
		return request;
	}

	public WebRtcMediaRequestImpl get(String requestId) {
		return requests.get(requestId);
	}

	public WebRtcMediaRequestImpl remove(String requestId) {
		return requests.remove(requestId);
	}
}
