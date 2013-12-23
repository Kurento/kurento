package com.kurento.demo.cpbrasil;

import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.WebRtcContentService;
import com.kurento.kmf.content.WebRtcContentSession;
import com.kurento.kmf.media.FaceOverlayFilter;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PointerDetectorFilter;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.WindowInEvent;
import com.kurento.kmf.media.params.internal.PointerDetectorWindowMediaParam;
import com.kurento.kmf.media.params.internal.PointerDetectorWindowMediaParam.PointerDetectorWindowMediaParamBuilder;

@WebRtcContentService(path = "/webRtcPointerDetectorLoopbackCPB")
public class CpbWebRtcPointerDetectorLoopback extends WebRtcContentHandler {

	@Override
	public void onContentRequest(WebRtcContentSession contentSession)
			throws Exception {
		MediaPipeline mp = contentSession.getMediaPipelineFactory().create();
		contentSession.releaseOnTerminate(mp);

		PointerDetectorFilter pointerDetectorFilter = mp
				.newPointerDetectorFilter().build();
		PointerDetectorWindowMediaParam window1 = new PointerDetectorWindowMediaParamBuilder(
				"window1", 50, 50, 50, 50).build();
		pointerDetectorFilter.addWindow(window1);
		final FaceOverlayFilter faceOverlayFilter = mp.newFaceOverlayFilter()
				.build();
		pointerDetectorFilter.connect(faceOverlayFilter);
		pointerDetectorFilter
				.addWindowInListener(new MediaEventListener<WindowInEvent>() {
					@Override
					public void onEvent(WindowInEvent event) {
						String imageUri = "/opt/img/fireman.png";
						faceOverlayFilter.setOverlayedImage(imageUri, 0.0F,
								0.6F, 1.2F, 0.8F);
					}
				});
		contentSession.start(faceOverlayFilter, pointerDetectorFilter);
	}

	@Override
	public void onContentStarted(WebRtcContentSession contentSession)
			throws Exception {
		super.onContentStarted(contentSession);
	}

}