/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

import static com.google.common.collect.Lists.newArrayList;

import java.net.URISyntaxException;
import java.util.Calendar;

import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.demo.cpbrazil.youtube.Videos;
import com.kurento.kmf.content.ContentCommand;
import com.kurento.kmf.content.ContentCommandResult;
import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.WebRtcContentService;
import com.kurento.kmf.content.WebRtcContentSession;
import com.kurento.kmf.media.ChromaFilter;
import com.kurento.kmf.media.FaceOverlayFilter;
import com.kurento.kmf.media.GStreamerFilter;
import com.kurento.kmf.media.MediaApiConfiguration;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PointerDetectorAdvFilter;
import com.kurento.kmf.media.RecorderEndpoint;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.WindowInEvent;
import com.kurento.kmf.media.params.internal.PointerDetectorWindowMediaParam;
import com.kurento.kmf.media.params.internal.PointerDetectorWindowMediaParam.PointerDetectorWindowMediaParamBuilder;
import com.kurento.kmf.media.params.internal.WindowParam;
import com.kurento.kmf.repository.RepositoryHttpRecorder;
import com.kurento.kmf.repository.RepositoryItem;

/**
 * Campus Party Brazil 2014 Kurento demo. This demo has the following pipeline:
 * 
 * WebRTC -> RateLimiter -> MirrorFilter -> PointerDetectorFilter ->
 * ChromaFilter -> FaceOverlayFilter -> Recorder
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.1
 */
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
	public GStreamerFilter rateLimiter;
	public PointerDetectorAdvFilter pointerDetectorAdvFilter;
	public FaceOverlayFilter faceOverlayFilter;
	public ChromaFilter chromaFilter;
	public RecorderEndpoint recorderEndpoint;

	// Global demo elements
	public String itemId;
	public String activeWindow;
	private String handlerUrl;
	private String recorderUrl;
	private int mario;
	private int count;

	@Autowired
	private MediaApiConfiguration config;

	@Override
	public void onContentRequest(final WebRtcContentSession contentSession)
			throws Exception {
		mario = 1;
		count = 1;
		String contentId = contentSession.getContentId();

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

		rateLimiter = mediaPipeline.newGStreamerFilter(
				"videorate max-rate=15 average-period=200000000").build();
		mirrorFilter = mediaPipeline.newGStreamerFilter("videoflip method=4")
				.build();
		chromaFilter = mediaPipeline.newChromaFilter(
				new WindowParam(100, 10, 500, 400)).build();
		pointerDetectorAdvFilter = mediaPipeline
				.newPointerDetectorAdvFilter(new WindowParam(5, 5, 50, 50))
				.withWindow(createStartWindow()).build();
		faceOverlayFilter = mediaPipeline.newFaceOverlayFilter().build();
		rateLimiter.connect(mirrorFilter);
		mirrorFilter.connect(pointerDetectorAdvFilter);
		pointerDetectorAdvFilter.connect(chromaFilter);
		chromaFilter.connect(faceOverlayFilter);

		pointerDetectorAdvFilter.addWindow(createFiwareWindow());
		pointerDetectorAdvFilter
				.addWindowInListener(new MediaEventListener<WindowInEvent>() {
					@Override
					public void onEvent(WindowInEvent event) {
						try {
							String windowId = event.getWindowId();
							if (windowId.equals(START)) {
								pointerDetectorAdvFilter.clearWindows();
								pointerDetectorAdvFilter
										.addWindow(createFiwareWindow());
								pointerDetectorAdvFilter
										.addWindow(createMarioWindow());
								pointerDetectorAdvFilter
										.addWindow(createDKWindow());
								pointerDetectorAdvFilter
										.addWindow(createSFWindow());
								pointerDetectorAdvFilter
										.addWindow(createSonicWindow());
								addRecorder(contentSession, recordOnRepository);
								recorderEndpoint.record();

							} else if (windowId.equals(SF)
									&& !activeWindow.equals(SF)) {
								if (count % 20 == 0) {
									setStarWars();
								} else if (count % 10 == 0) {
									setPirates();
								} else {
									faceOverlayFilter.setOverlayedImage(
											handlerUrl + "/img/masks/sf.png",
											-0.35F, -0.5F, 1.6F, 1.6F);
									chromaFilter.setBackground(handlerUrl
											+ "/img/background/sf.jpg");
								}
								count++;
								if (activeWindow.equals(START)) {
									createTrashAndYouTubeWindow();
								}
								activeWindow = SF;

							} else if (windowId.equals(MARIO)
									&& !activeWindow.equals(MARIO)) {
								if (count % 20 == 0) {
									setStarWars();
								} else if (count % 10 == 0) {
									setPirates();
								} else {
									chromaFilter.setBackground(handlerUrl
											+ "/img/background/mario.jpg");
									if (mario % 2 == 0) {
										faceOverlayFilter
												.setOverlayedImage(
														handlerUrl
																+ "/img/masks/mario-wings.png",
														-0.35F, -1.2F, 1.6F,
														1.6F);
									} else {
										faceOverlayFilter
												.setOverlayedImage(
														handlerUrl
																+ "/img/masks/mario.png",
														-0.3F, -0.6F, 1.6F,
														1.6F);
									}
								}
								count++;
								mario++;
								if (activeWindow.equals(START)) {
									createTrashAndYouTubeWindow();
								}
								activeWindow = MARIO;

							} else if (windowId.equals(DK)
									&& !activeWindow.equals(DK)) {
								if (count % 20 == 0) {
									setStarWars();
								} else if (count % 10 == 0) {
									setPirates();
								} else {
									faceOverlayFilter.setOverlayedImage(
											handlerUrl + "/img/masks/dk.png",
											-0.35F, -0.5F, 1.6F, 1.6F);
									chromaFilter.setBackground(handlerUrl
											+ "/img/background/dk.jpg");
								}
								if (activeWindow.equals(START)) {
									createTrashAndYouTubeWindow();
								}
								count++;
								activeWindow = DK;

							} else if (windowId.equals(SONIC)
									&& !activeWindow.equals(SONIC)) {
								if (count % 20 == 0) {
									setStarWars();
								} else if (count % 10 == 0) {
									setPirates();
								} else {
									faceOverlayFilter
											.setOverlayedImage(handlerUrl
													+ "/img/masks/sonic.png",
													-0.5F, -0.5F, 1.7F, 1.7F);
									chromaFilter.setBackground(handlerUrl
											+ "/img/background/sonic.jpg");
								}
								count++;
								if (activeWindow.equals(START)) {
									createTrashAndYouTubeWindow();
								}
								activeWindow = SONIC;

							} else if (windowId.equals(YOUTUBE)
									|| windowId.equals(TRASH)) {
								chromaFilter.unsetBackground();
								pointerDetectorAdvFilter.clearWindows();
								pointerDetectorAdvFilter
										.addWindow(createFiwareWindow());
								faceOverlayFilter.unsetOverlayedImage();
								pointerDetectorAdvFilter
										.addWindow(createStartWindow());
								// recorderEndpoint.stop();
								recorderEndpoint.release();
								if (windowId.equals(YOUTUBE)) {
									String recordUrl = handlerUrl
											+ (recordOnRepository ? "/playerRepository/"
													: "/cpbPlayer/") + itemId;
									getLogger().info("recordUrl " + recordUrl);
									Videos.upload(
											recordUrl,
											newArrayList("FI-WARE", "Kurento",
													"FUN-LAB", "GSyC", "URJC",
													"Campus Party", "WebRTC",
													"Software Engineering",
													"Augmented Reality",
													"Computer Vision",
													"Super Mario", "Sonic",
													"Street Fighter",
													"Donkey Kong"));
								}
							}

						} catch (URISyntaxException e) {
							getLogger().error(e.getMessage());
							e.printStackTrace();
						}
					}
				});
		contentSession.start(faceOverlayFilter, rateLimiter);
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

		faceOverlayFilter.connect(recorderEndpoint);
	}

	private PointerDetectorWindowMediaParam createStartWindow()
			throws URISyntaxException {
		activeWindow = START;
		return new PointerDetectorWindowMediaParamBuilder(START, 100, 100, 280,
				380).withImage(handlerUrl + "/img/buttons/start.png").build();
	}

	private PointerDetectorWindowMediaParam createMarioWindow()
			throws URISyntaxException {
		return new PointerDetectorWindowMediaParamBuilder(MARIO, 100, 100, 540,
				0).withImage(handlerUrl + "/img/buttons/mario.png").build();
	}

	private PointerDetectorWindowMediaParam createDKWindow()
			throws URISyntaxException {
		return new PointerDetectorWindowMediaParamBuilder(DK, 100, 100, 540,
				126).withImage(handlerUrl + "/img/buttons/dk.png").build();
	}

	private PointerDetectorWindowMediaParam createSFWindow()
			throws URISyntaxException {
		return new PointerDetectorWindowMediaParamBuilder(SF, 100, 100, 540,
				252).withImage(handlerUrl + "/img/buttons/sf.png").build();
	}

	private PointerDetectorWindowMediaParam createSonicWindow()
			throws URISyntaxException {
		return new PointerDetectorWindowMediaParamBuilder(SONIC, 100, 100, 540,
				380).withImage(handlerUrl + "/img/buttons/sonic.png").build();
	}

	private PointerDetectorWindowMediaParam createYouTubeWindow()
			throws URISyntaxException {
		return new PointerDetectorWindowMediaParamBuilder(YOUTUBE, 100, 100, 0,
				380).withImage(handlerUrl + "/img/buttons/youtube.png").build();
	}

	private PointerDetectorWindowMediaParam createTrashWindow()
			throws URISyntaxException {
		return new PointerDetectorWindowMediaParamBuilder(TRASH, 100, 100, 0,
				190).withImage(handlerUrl + "/img/buttons/trash.png").build();
	}

	private PointerDetectorWindowMediaParam createFiwareWindow()
			throws URISyntaxException {
		return new PointerDetectorWindowMediaParamBuilder("fiware", 40, 180,
				230, 0).withImage(handlerUrl + "/img/buttons/fiware2.png")
				.build();
	}

	private void createTrashAndYouTubeWindow() throws URISyntaxException {
		pointerDetectorAdvFilter.addWindow(createTrashWindow());
		pointerDetectorAdvFilter.addWindow(createYouTubeWindow());
	}

	@Override
	public ContentCommandResult onContentCommand(
			WebRtcContentSession contentSession, ContentCommand contentCommand)
			throws Exception {

		if (contentCommand.getType().equalsIgnoreCase("calibrate")) {
			pointerDetectorAdvFilter.trackcolourFromCalibrationRegion();
		}
		return new ContentCommandResult(contentCommand.getData());
	}

	private void setStarWars() {
		faceOverlayFilter.setOverlayedImage(handlerUrl
				+ "/img/masks/darthvader.png", -0.5F, -0.5F, 1.7F, 1.7F);
		chromaFilter
				.setBackground(handlerUrl + "/img/background/deathstar.jpg");
	}

	private void setPirates() {
		faceOverlayFilter.setOverlayedImage(handlerUrl + "/img/masks/jack.png",
				-0.4F, -0.4F, 1.7F, 1.7F);
		chromaFilter.setBackground(handlerUrl + "/img/background/pirates.jpg");
	}

}
