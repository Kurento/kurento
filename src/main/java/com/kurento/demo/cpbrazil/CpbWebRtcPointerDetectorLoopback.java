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

@WebRtcContentService(path = "/webRtcPointerDetectorLoopbackCPB")
public class CpbWebRtcPointerDetectorLoopback extends WebRtcContentHandler {

	private static String START = "start";
	private static String MARIO = "mario";
	private static String STARWARS = "starwars";
	private static String FIREFIGHTER = "firefighter";
	private static String TRASH = "trash";
	private static String YOUTUBE = "youtube";

	// MediaPipeline and MediaElements
	public MediaPipeline mediaPipeline;
	public GStreamerFilter mirrorFilter;
	public PointerDetectorFilter pointerDetectorFilter;
	public FaceOverlayFilter faceOverlayFilter;
	public RecorderEndpoint recorderEndpoint;
	public Repository repository;

	// ItemId
	public String itemId;

	public String activeWindow;

	// Uploader
	public UploadVideoYouTube videoYouTube;

	@Autowired
	private MediaApiConfiguration config;

	public String mediaUrl;

	@Override
	public void onContentRequest(final WebRtcContentSession contentSession)
			throws Exception {
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

		mediaUrl = contentSession.getHttpServletRequest().getScheme() + "://"
				+ config.getServerAddress() + "/";

		// mediaUrl = contentSession.getHttpServletRequest().getScheme() + "://"
		// + config.getHandlerAddress() + ":"
		// + contentSession.getHttpServletRequest().getServerPort()
		// + contentSession.getHttpServletRequest().getContextPath()
		// + "/cpbPlayer/";

		// mediaUrl = contentSession.getHttpServletRequest().getScheme() + "://"
		// + config.getHandlerAddress() + ":"
		// + contentSession.getHttpServletRequest().getServerPort();

		getLogger().info("-------------- activeWindow " + activeWindow);

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
										.addWindow(createStarWarsWindow());
								pointerDetectorFilter
										.addWindow(createFirefighterWindow());

								addRecorder(contentSession);
								recorderEndpoint.record();
								getLogger().info(
										"**************************** windowId "
												+ windowId + " activeWindow "
												+ activeWindow);

							} else if (windowId.equals(FIREFIGHTER)
									&& !activeWindow.equals(FIREFIGHTER)) {
								getLogger().info(
										"**************************** windowId "
												+ windowId + " activeWindow "
												+ activeWindow);
								faceOverlayFilter.setOverlayedImage(
										"/opt/img/firefighter_hat.png", 0.0F,
										-0.6F, 1.2F, 0.8F);
								if (activeWindow.equals(START)) {
									pointerDetectorFilter
											.addWindow(createTrashWindow());
									pointerDetectorFilter
											.addWindow(createYouTubeWindow());
								}
								activeWindow = FIREFIGHTER;

							} else if (windowId.equals(MARIO)
									&& !activeWindow.equals(MARIO)) {
								faceOverlayFilter.setOverlayedImage(
										"/opt/img/mario_hat.png", 0.0F, -0.6F,
										1.2F, 0.8F);
								if (activeWindow.equals(START)) {
									pointerDetectorFilter
											.addWindow(createTrashWindow());
									pointerDetectorFilter
											.addWindow(createYouTubeWindow());
									getLogger().info(
											"**************************** windowId "
													+ windowId
													+ " activeWindow "
													+ activeWindow);
								}
								activeWindow = MARIO;

							} else if (windowId.equals(STARWARS)
									&& !activeWindow.equals(STARWARS)) {
								faceOverlayFilter.setOverlayedImage(
										"/opt/img/starwars_hat.png", -0.3F,
										-0.5F, 1.8F, 1.8F);
								if (activeWindow.equals(START)) {
									pointerDetectorFilter
											.addWindow(createTrashWindow());
									pointerDetectorFilter
											.addWindow(createYouTubeWindow());
									getLogger().info(
											"**************************** windowId "
													+ windowId
													+ " activeWindow "
													+ activeWindow);
								}
								activeWindow = STARWARS;

							} else if (windowId.equals(YOUTUBE)
									|| windowId.equals(TRASH)) {
								pointerDetectorFilter.clearWindows();
								faceOverlayFilter.setOverlayedImage(
										"/opt/img/1px_trans.png", 0.0F, 0.0F,
										0.0F, 0.0F);
								pointerDetectorFilter
										.addWindow(createStartWindow());
								recorderEndpoint.stop();
								recorderEndpoint.release();
								if (windowId.equals(YOUTUBE)) {
									// TODO improve
									getLogger().info(
											"............. " + mediaUrl
													+ itemId);
									videoYouTube.uploadVideo(mediaUrl + itemId);
								}
								getLogger().info(
										"**************************** windowId "
												+ windowId + " activeWindow "
												+ activeWindow);

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

		// RepositoryItem repositoryItem =
		// repository.createRepositoryItem(itemId);
		// RepositoryHttpRecorder recorder = repositoryItem
		// .createRepositoryHttpRecorder();
		// recorderEndpoint = mediaPipeline.newRecorderEndpoint(
		// mediaUrl + recorder.getURL()).build();

		recorderEndpoint = mediaPipeline.newRecorderEndpoint(
				"file:///tmp/" + itemId).build();
		pointerDetectorFilter.connect(recorderEndpoint);
	}

	private PointerDetectorWindowMediaParam createStartWindow()
			throws URISyntaxException {
		activeWindow = START;
		return new PointerDetectorWindowMediaParamBuilder(START, 100, 100, 280,
				380).withActiveImage("/opt/img/play_inactive.png")
				.withInactiveImage("/opt/img/play_inactive.png").build();
	}

	private PointerDetectorWindowMediaParam createMarioWindow()
			throws URISyntaxException {
		return new PointerDetectorWindowMediaParamBuilder(MARIO, 100, 100, 540,
				0).withActiveImage("/opt/img/mario_inactive.png")
				.withInactiveImage("/opt/img/mario_inactive.png").build();
	}

	private PointerDetectorWindowMediaParam createStarWarsWindow()
			throws URISyntaxException {
		return new PointerDetectorWindowMediaParamBuilder(STARWARS, 100, 100,
				540, 190).withInactiveImage("/opt/img/vader_inactive.png")
				.withInactiveImage("/opt/img/vader_inactive.png").build();
	}

	private PointerDetectorWindowMediaParam createFirefighterWindow()
			throws URISyntaxException {
		return new PointerDetectorWindowMediaParamBuilder(FIREFIGHTER, 100,
				100, 540, 380)
				.withActiveImage("/opt/img/firefighter_inactive.png")
				.withInactiveImage("/opt/img/firefighter_inactive.png").build();
	}

	private PointerDetectorWindowMediaParam createYouTubeWindow()
			throws URISyntaxException {
		return new PointerDetectorWindowMediaParamBuilder(YOUTUBE, 100, 100, 0,
				380).withActiveImage("/opt/img/youtube_inactive.png")
				.withInactiveImage("/opt/img/youtube_inactive.png").build();
	}

	private PointerDetectorWindowMediaParam createTrashWindow()
			throws URISyntaxException {
		return new PointerDetectorWindowMediaParamBuilder(TRASH, 100, 100, 0,
				190).withActiveImage("/opt/img/trash_inactive.png")
				.withInactiveImage("/opt/img/trash_inactive.png").build();
	}

}