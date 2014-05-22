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
package com.kurento.demo.cplondon;

import com.kurento.kmf.content.RtpContentHandler;
import com.kurento.kmf.content.RtpContentService;
import com.kurento.kmf.content.RtpContentSession;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.RtpEndpoint;
import com.kurento.kmf.media.ZBarFilter;
import com.kurento.kmf.media.factory.MediaPipelineFactory;

@RtpContentService(name = "CpRtcRtpZbarHandler", path = "/cpRtpZbar")
public class CpRtcRtpZbarHandler extends RtpContentHandler {

	public static ZBarFilter sharedFilterReference;

	@Override
	public void onContentRequest(RtpContentSession session) throws Exception {
		MediaPipelineFactory mpf = session.getMediaPipelineFactory();
		MediaPipeline mp = mpf.create();
		session.releaseOnTerminate(mp);
		ZBarFilter filter = mp.newZBarFilter().build();
		RtpEndpoint rtpEndpoint = mp.newRtpEndpoint().build();
		rtpEndpoint.connect(filter);
		rtpEndpoint.connect(rtpEndpoint);
		session.start(rtpEndpoint);
		sharedFilterReference = filter;
	}

}
