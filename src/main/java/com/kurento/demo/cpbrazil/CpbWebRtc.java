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
package com.kurento.demo.cpbrazil;

import java.net.URISyntaxException;
import java.util.Calendar;

import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.WebRtcContentService;
import com.kurento.kmf.content.WebRtcContentSession;
import com.kurento.kmf.media.ChromaFilter;
import com.kurento.kmf.media.FaceOverlayFilter;
import com.kurento.kmf.media.GStreamerFilter;
import com.kurento.kmf.media.MediaApiConfiguration;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PointerDetectorFilter;
import com.kurento.kmf.media.RecorderEndpoint;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.WindowInEvent;
import com.kurento.kmf.media.params.internal.PointerDetectorWindowMediaParam;
import com.kurento.kmf.media.params.internal.PointerDetectorWindowMediaParam.PointerDetectorWindowMediaParamBuilder;
import com.kurento.kmf.media.params.internal.WindowParam;
import com.kurento.kmf.repository.RepositoryHttpRecorder;
import com.kurento.kmf.repository.RepositoryItem;

@WebRtcContentService(path = "/cpbWebRtc/*")
public class CpbWebRtc extends WebRtcContentHandler {

	// Identifier of windows
	private static String START = "start";
	private static String SF = "sf";
	private static String MARIO = "mario";
	private static String DK = "DK";
	private static String SONIC = "sonic";
	private static String TRASH = "trash";
	private static String YOUTUBE = "youtube";

	// MediaPipeline and MediaElements
	public MediaPipeline mediaPipeline;
	public GStreamerFilter mirrorFilter;
	public PointerDetectorFilter pointerDetectorFilter;
	public FaceOverlayFilter faceOverlayFilter;
	public ChromaFilter chromaFilter;
	public RecorderEndpoint recorderEndpoint;

	// Global demo elements
	public String itemId;
	public String activeWindow;
	private String handlerUrl;
	private String recorderUrl;

	@Autowired
	private MediaApiConfiguration config;

	@Override
	public void onContentRequest(final WebRtcContentSession contentSession)
			throws Exception {
		String contentId = contentSession.getContentId();

		final UploadVideoYouTube videoYouTube = new UploadVideoYouTube();
		final boolean recordOnRepository = contentId != null
				&& contentId.equalsIgnoreCase("repositoryRecorder");
		recorderUrl = contentSession.getHttpServletRequest().getScheme()
				+ "://" + config.getHandlerAddress() + ":"
				+ contentSession.getHttpServletRequest().getServerPort();
		handlerUrl = recorderUrl
				+ contentSession.getHttpServletRequest().getContextPath();
		getLogger().info("handlerUrl " + handlerUrl);

		mediaPipeline = contentSession.getMediaPipelineFactory().create();
		contentSession.releaseOnTerminate(mediaPipeline);

		mirrorFilter = mediaPipeline.newGStreamerFilter("videoflip method=4")
				.build();
		chromaFilter = mediaPipeline.newChromaFilter(
				new WindowParam(100, 10, 20, 20)).build();
		pointerDetectorFilter = mediaPipeline.newPointerDetectorFilter()
				.withWindow(createStartWindow()).build();
		faceOverlayFilter = mediaPipeline.newFaceOverlayFilter().build();
		mirrorFilter.connect(chromaFilter);
		chromaFilter.connect(faceOverlayFilter);
		faceOverlayFilter.connect(pointerDetectorFilter);

		pointerDetectorFilter
				.addWindowInListener(new MediaEventListener<WindowInEvent>() {
					@Override
					public void onEvent(WindowInEvent event) {
						try {
							String windowId = event.getWindowId();
							if (windowId.equals(START)) {
								// chromaFilter.setBackground(handlerUrl
								// + "/img/transparent-1px.png");
								pointerDetectorFilter.clearWindows();
								pointerDetectorFilter
										.addWindow(createMarioWindow());
								pointerDetectorFilter
										.addWindow(createDKWindow());
								pointerDetectorFilter
										.addWindow(createSFWindow());
								pointerDetectorFilter
										.addWindow(createSonicWindow());
								addRecorder(contentSession, recordOnRepository);
								recorderEndpoint.record();

							} else if (windowId.equals(SF)
									&& !activeWindow.equals(SF)) {
								faceOverlayFilter.setOverlayedImage(handlerUrl
										+ "/img/masks/sf.png", -0.5F, -0.5F,
										1.6F, 1.6F);
								chromaFilter.setBackground(handlerUrl
										+ "/img/background/sf.jpg");
								if (activeWindow.equals(START)) {
									createTrashAndYouTubeWindow();
								}
								activeWindow = SF;

							} else if (windowId.equals(MARIO)
									&& !activeWindow.equals(MARIO)) {
								faceOverlayFilter.setOverlayedImage(handlerUrl
										+ "/img/masks/mario.png", -0.3F, -0.5F,
										1.6F, 1.6F);
								chromaFilter.setBackground(handlerUrl
										+ "/img/background/mario.jpg");
								if (activeWindow.equals(START)) {
									createTrashAndYouTubeWindow();
								}
								activeWindow = MARIO;

							} else if (windowId.equals(DK)
									&& !activeWindow.equals(DK)) {
								faceOverlayFilter.setOverlayedImage(handlerUrl
										+ "/img/masks/dk.png", -0.5F, -0.5F,
										1.6F, 1.6F);
								chromaFilter.setBackground(handlerUrl
										+ "/img/background/dk.jpg");
								if (activeWindow.equals(START)) {
									createTrashAndYouTubeWindow();
								}
								activeWindow = DK;

							} else if (windowId.equals(SONIC)
									&& !activeWindow.equals(SONIC)) {
								faceOverlayFilter.setOverlayedImage(handlerUrl
										+ "/img/masks/sonic.png", -0.5F, -0.5F,
										1.7F, 1.7F);
								chromaFilter.setBackground(handlerUrl
										+ "/img/background/sonic.jpg");
								if (activeWindow.equals(START)) {
									createTrashAndYouTubeWindow();
								}
								activeWindow = DK;

							} else if (windowId.equals(YOUTUBE)
									|| windowId.equals(TRASH)) {
								pointerDetectorFilter.clearWindows();
								faceOverlayFilter.setOverlayedImage(handlerUrl
										+ "/img/transparent-1px.png", 0.0F,
										0.0F, 0.0F, 0.0F);
								pointerDetectorFilter
										.addWindow(createStartWindow());
								recorderEndpoint.stop();
								recorderEndpoint.release();
								if (windowId.equals(YOUTUBE)) {
									String recordUrl = handlerUrl
											+ (recordOnRepository ? "/playerRepository/"
													: "/cpbPlayer/") + itemId;
									getLogger().info("recordUrl " + recordUrl);
									videoYouTube.uploadVideo(recordUrl);
								}
							}

						} catch (URISyntaxException e) {
							getLogger().error(e.getMessage());
							e.printStackTrace();
						}
					}
				});
		contentSession.start(pointerDetectorFilter, mirrorFilter);
	}

	@Override
	public void onContentStarted(WebRtcContentSession contentSession)
			throws Exception {
		super.onContentStarted(contentSession);
	}

	private void addRecorder(WebRtcContentSession contentSession,
			boolean repositoryRecorder) {
		itemId = "campus-party-" + Calendar.getInstance().getTimeInMillis();

		if (repositoryRecorder) {
			RepositoryItem repositoryItem = contentSession.getRepository()
					.createRepositoryItem(itemId);
			RepositoryHttpRecorder recorder = repositoryItem
					.createRepositoryHttpRecorder();
			getLogger().info(
					"repositoryRecorderUrl + recorder.getURL()" + recorderUrl
							+ recorder.getURL());
			recorderEndpoint = mediaPipeline.newRecorderEndpoint(
					recorderUrl + recorder.getURL()).build();
		} else {
			recorderEndpoint = mediaPipeline.newRecorderEndpoint(
					"file:///tmp/" + itemId).build();
		}

		pointerDetectorFilter.connect(recorderEndpoint);
	}

	private PointerDetectorWindowMediaParam createStartWindow()
			throws URISyntaxException {
		activeWindow = START;
		return new PointerDetectorWindowMediaParamBuilder(START, 100, 100, 280,
				380).withActiveImage(handlerUrl + "/img/buttons/start.png")
				.withInactiveImage(handlerUrl + "/img/buttons/start.png")
				.build();
	}

	private PointerDetectorWindowMediaParam createMarioWindow()
			throws URISyntaxException {
		return new PointerDetectorWindowMediaParamBuilder(MARIO, 100, 100, 540,
				0).withActiveImage(handlerUrl + "/img/buttons/mario.png")
				.withInactiveImage(handlerUrl + "/img/buttons/mario.png")
				.build();
	}

	private PointerDetectorWindowMediaParam createDKWindow()
			throws URISyntaxException {
		return new PointerDetectorWindowMediaParamBuilder(DK, 100, 100, 540,
				126).withActiveImage(handlerUrl + "/img/buttons/dk.png")
				.withInactiveImage(handlerUrl + "/img/buttons/dk.png").build();
	}

	private PointerDetectorWindowMediaParam createSFWindow()
			throws URISyntaxException {
		return new PointerDetectorWindowMediaParamBuilder(SF, 100, 100, 540,
				252).withActiveImage(handlerUrl + "/img/buttons/sf.png")
				.withInactiveImage(handlerUrl + "/img/buttons/sf.png").build();
	}

	private PointerDetectorWindowMediaParam createSonicWindow()
			throws URISyntaxException {
		return new PointerDetectorWindowMediaParamBuilder(SONIC, 100, 100, 540,
				380).withActiveImage(handlerUrl + "/img/buttons/sonic.png")
				.withInactiveImage(handlerUrl + "/img/buttons/sonic.png")
				.build();
	}

	private PointerDetectorWindowMediaParam createYouTubeWindow()
			throws URISyntaxException {
		return new PointerDetectorWindowMediaParamBuilder(YOUTUBE, 100, 100, 0,
				380).withActiveImage(handlerUrl + "/img/buttons/youtube.png")
				.withInactiveImage(handlerUrl + "/img/buttons/youtube.png")
				.build();
	}

	private PointerDetectorWindowMediaParam createTrashWindow()
			throws URISyntaxException {
		return new PointerDetectorWindowMediaParamBuilder(TRASH, 100, 100, 0,
				190).withActiveImage(handlerUrl + "/img/buttons/trash.png")
				.withInactiveImage(handlerUrl + "/img/buttons/trash.png")
				.build();
	}

	private void createTrashAndYouTubeWindow() throws URISyntaxException {
		pointerDetectorFilter.addWindow(createTrashWindow());
		pointerDetectorFilter.addWindow(createYouTubeWindow());
	}

}