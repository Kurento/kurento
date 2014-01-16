package com.kurento.demo.cpbrazil;

import java.net.URISyntaxException;
import java.util.Calendar;

import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.WebRtcContentService;
import com.kurento.kmf.content.WebRtcContentSession;
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
import com.kurento.kmf.repository.Repository;

@WebRtcContentService(path = "/cpbWebRtc")
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
	public RecorderEndpoint recorderEndpoint;
	public Repository repository;

	// Demo elements
	public String itemId;
	public String activeWindow;
	public UploadVideoYouTube videoYouTube;
	public String handlerUrl;

	@Autowired
	private MediaApiConfiguration config;

	@Override
	public void onContentRequest(final WebRtcContentSession contentSession)
			throws Exception {
		handlerUrl = contentSession.getHttpServletRequest().getScheme() + "://"
				+ config.getHandlerAddress() + ":"
				+ contentSession.getHttpServletRequest().getServerPort()
				+ contentSession.getHttpServletRequest().getContextPath();

		videoYouTube = new UploadVideoYouTube();

		mediaPipeline = contentSession.getMediaPipelineFactory().create();
		contentSession.releaseOnTerminate(mediaPipeline);

		repository = contentSession.getRepository();

		mirrorFilter = mediaPipeline.newGStreamerFilter("videoflip method=4")
				.build();

		pointerDetectorFilter = mediaPipeline.newPointerDetectorFilter()
				.withWindow(createStartWindow()).build();

		faceOverlayFilter = mediaPipeline.newFaceOverlayFilter().build();

		mirrorFilter.connect(faceOverlayFilter);
		faceOverlayFilter.connect(pointerDetectorFilter);

		pointerDetectorFilter
				.addWindowInListener(new MediaEventListener<WindowInEvent>() {
					@Override
					public void onEvent(WindowInEvent event) {
						try {
							String windowId = event.getWindowId();
							if (windowId.equals(START)) {
								pointerDetectorFilter.clearWindows();
								pointerDetectorFilter
										.addWindow(createMarioWindow());
								pointerDetectorFilter
										.addWindow(createDKWindow());
								pointerDetectorFilter
										.addWindow(createSFWindow());
								pointerDetectorFilter
										.addWindow(createSonicWindow());
								addRecorder(contentSession);
								recorderEndpoint.record();

							} else if (windowId.equals(SF)
									&& !activeWindow.equals(SF)) {
								faceOverlayFilter.setOverlayedImage(handlerUrl
										+ "/img/masks/sf.png", -0.5F, -0.5F,
										1.6F, 1.6F);
								if (activeWindow.equals(START)) {
									createTrashAndYouTubeWindow();
								}
								activeWindow = SF;

							} else if (windowId.equals(MARIO)
									&& !activeWindow.equals(MARIO)) {
								faceOverlayFilter.setOverlayedImage(handlerUrl
										+ "/img/masks/mario.png", -0.3F, -0.5F,
										1.6F, 1.6F);
								if (activeWindow.equals(START)) {
									createTrashAndYouTubeWindow();
								}
								activeWindow = MARIO;

							} else if (windowId.equals(DK)
									&& !activeWindow.equals(DK)) {
								faceOverlayFilter.setOverlayedImage(handlerUrl
										+ "/img/masks/dk.png", -0.5F, -0.5F,
										1.6F, 1.6F);
								if (activeWindow.equals(START)) {
									createTrashAndYouTubeWindow();
								}
								activeWindow = DK;

							} else if (windowId.equals(SONIC)
									&& !activeWindow.equals(SONIC)) {
								faceOverlayFilter.setOverlayedImage(handlerUrl
										+ "/img/masks/sonic.png", -0.5F, -0.5F,
										1.7F, 1.7F);
								if (activeWindow.equals(START)) {
									createTrashAndYouTubeWindow();
								}
								activeWindow = DK;

							} else if (windowId.equals(YOUTUBE)
									|| windowId.equals(TRASH)) {
								pointerDetectorFilter.clearWindows();
								faceOverlayFilter.setOverlayedImage(handlerUrl
										+ "masks/1px_trans.png", 0.0F, 0.0F,
										0.0F, 0.0F);
								pointerDetectorFilter
										.addWindow(createStartWindow());
								recorderEndpoint.stop();
								recorderEndpoint.release();
								if (windowId.equals(YOUTUBE)) {
									videoYouTube.uploadVideo(handlerUrl
											+ "/cpbPlayer/" + itemId);
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

	private void addRecorder(WebRtcContentSession contentSession) {
		itemId = "campus-party-" + Calendar.getInstance().getTimeInMillis();
		recorderEndpoint = mediaPipeline.newRecorderEndpoint(
				"file:///tmp/" + itemId).build();
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