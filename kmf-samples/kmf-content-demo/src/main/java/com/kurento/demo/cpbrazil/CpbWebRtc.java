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

import com.kurento.demo.cpbrazil.CpbWindows.Windows;
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
import com.kurento.kmf.media.PointerDetectorWindowMediaParam;
import com.kurento.kmf.media.RecorderEndpoint;
import com.kurento.kmf.media.WebRtcEndpoint;
import com.kurento.kmf.media.WindowParam;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.WindowInEvent;
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

	// MediaPipeline and MediaElements
	public MediaPipeline mediaPipeline;
	public GStreamerFilter mirrorFilter;
	public GStreamerFilter rateLimiter;
	public PointerDetectorAdvFilter pointerDetectorAdvFilter;
	public FaceOverlayFilter faceOverlayFilter;
	public ChromaFilter chromaFilter;
	public RecorderEndpoint recorderEndpoint;

	// Token to upload videos to Kurento Brazil Demo playlist
	private static final String PLAYLIST_TOKEN = "PL58tWS2XjtialwG-eWDYoFwQpHTd5vDEE";

	// Global demo elements
	public CpbWindows cpbWindows;
	public String itemId;
	public Windows activeWindow;
	private String handlerUrl;
	private String recorderUrl;
	private int mario = 1;
	private int count = 1;

	@Autowired
	private MediaApiConfiguration config;

	@Override
	public void onContentRequest(final WebRtcContentSession contentSession)
			throws Exception {
		String contentId = contentSession.getContentId();

		final boolean recordOnRepository = contentId != null
				&& contentId.equalsIgnoreCase("repositoryRecorder");
		recorderUrl = contentSession.getHttpServletRequest().getScheme()
				+ "://" + config.getHandlerAddress() + ":"
				+ contentSession.getHttpServletRequest().getServerPort();
		handlerUrl = recorderUrl
				+ contentSession.getHttpServletRequest().getContextPath();
		cpbWindows = new CpbWindows(handlerUrl);

		mediaPipeline = contentSession.getMediaPipelineFactory().create();
		contentSession.releaseOnTerminate(mediaPipeline);

		rateLimiter = mediaPipeline.newGStreamerFilter(
				"videorate max-rate=15 average-period=200000000").build();
		mirrorFilter = mediaPipeline.newGStreamerFilter("videoflip method=4")
				.build();

		chromaFilter = mediaPipeline.newChromaFilter(
				new WindowParam(100, 10, 500, 400)).build();
		pointerDetectorAdvFilter = mediaPipeline.newPointerDetectorAdvFilter(
				new WindowParam(5, 5, 50, 50)).build();
		pointerDetectorAdvFilter.addWindow(cpbWindows.start);
		activeWindow = Windows.START;
		faceOverlayFilter = mediaPipeline.newFaceOverlayFilter().build();
		rateLimiter.connect(mirrorFilter);
		mirrorFilter.connect(pointerDetectorAdvFilter);
		pointerDetectorAdvFilter.connect(chromaFilter);
		chromaFilter.connect(faceOverlayFilter);

		pointerDetectorAdvFilter.addWindow(cpbWindows.fiware);
		pointerDetectorAdvFilter
				.addWindowInListener(new MediaEventListener<WindowInEvent>() {
					@Override
					public void onEvent(WindowInEvent event) {
						try {
							Windows windowId = Windows.valueOf(event
									.getWindowId());
							switch (windowId) {
							case DK:
								setDK();
								break;
							case FIWARE:
								break;
							case MARIO:
								setMario();
								break;
							case SF:
								setSF();
								break;
							case SONIC:
								setSonic();
								break;
							case START:
								addRecorder(contentSession, recordOnRepository);
								setStart();
								break;
							case TRASH:
							case YOUTUBE:
								setEnding(windowId, recordOnRepository);
								break;
							default:
								break;
							}
						} catch (URISyntaxException e) {
							e.printStackTrace();
						}
					}
				});
		WebRtcEndpoint webRtcEndpoint = mediaPipeline.newWebRtcEndpoint()
				.build();
		webRtcEndpoint.connect(rateLimiter);
		faceOverlayFilter.connect(webRtcEndpoint);
		contentSession.start(webRtcEndpoint);
	}

	@Override
	public ContentCommandResult onContentCommand(
			WebRtcContentSession contentSession, ContentCommand contentCommand)
			throws Exception {
		if (contentCommand.getType().equalsIgnoreCase("calibrate")) {
			pointerDetectorAdvFilter.trackColorFromCalibrationRegion();
		}
		return new ContentCommandResult(contentCommand.getData());
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

	private void setStart() {
		pointerDetectorAdvFilter.clearWindows();
		pointerDetectorAdvFilter.addWindow(cpbWindows.fiware);
		pointerDetectorAdvFilter.addWindow(cpbWindows.mario);
		pointerDetectorAdvFilter.addWindow(cpbWindows.dk);
		pointerDetectorAdvFilter.addWindow(cpbWindows.sf);
		pointerDetectorAdvFilter.addWindow(cpbWindows.sonic);
		recorderEndpoint.record();
	}

	private void checkFirstTime() {
		if (activeWindow.equals(Windows.START)) {
			pointerDetectorAdvFilter.addWindow(cpbWindows.trash);
			pointerDetectorAdvFilter.addWindow(cpbWindows.youtube);
		}
	}

	private boolean checkEasterEggs() {
		boolean isEasterEgg = false;
		if (count % 20 == 0) {
			// Each 20 times (20, 40, ...) Darth Vader hat/background is
			// shown
			setStarWars();
			isEasterEgg = true;
		} else if (count % 10 == 0) {
			// Each 10 times (10, 30, ...) the Jack Sparrow hat/background is
			// shown
			setPirates();
			isEasterEgg = true;
		}
		count++;
		return isEasterEgg;
	}

	private void setSF() throws URISyntaxException {
		if (!activeWindow.equals(Windows.SF) && !checkEasterEggs()) {
			faceOverlayFilter.setOverlayedImage(handlerUrl
					+ "/img/masks/sf.png", -0.35F, -0.5F, 1.6F, 1.6F);
			chromaFilter.setBackground(handlerUrl + "/img/background/sf.jpg");
			checkFirstTime();
			activeWindow = Windows.SF;
		}
	}

	private void setMario() throws URISyntaxException {
		if (!activeWindow.equals(Windows.MARIO) && !checkEasterEggs()) {
			chromaFilter
					.setBackground(handlerUrl + "/img/background/mario.jpg");
			// Mario Easter Egg (a different mask each time)
			if (mario % 2 == 0) {
				faceOverlayFilter.setOverlayedImage(handlerUrl
						+ "/img/masks/mario-wings.png", -0.35F, -1.2F, 1.6F,
						1.6F);
			} else {
				faceOverlayFilter.setOverlayedImage(handlerUrl
						+ "/img/masks/mario.png", -0.3F, -0.6F, 1.6F, 1.6F);
			}
			mario++;
			checkFirstTime();
			activeWindow = Windows.MARIO;
		}
	}

	private void setDK() throws URISyntaxException {
		if (!activeWindow.equals(Windows.DK) && !checkEasterEggs()) {
			faceOverlayFilter.setOverlayedImage(handlerUrl
					+ "/img/masks/dk.png", -0.35F, -0.5F, 1.6F, 1.6F);
			chromaFilter.setBackground(handlerUrl + "/img/background/dk.jpg");
			checkFirstTime();
			activeWindow = Windows.DK;
		}
	}

	private void setSonic() throws URISyntaxException {
		if (!activeWindow.equals(Windows.SONIC) && !checkEasterEggs()) {
			faceOverlayFilter.setOverlayedImage(handlerUrl
					+ "/img/masks/sonic.png", -0.5F, -0.5F, 1.7F, 1.7F);
			chromaFilter
					.setBackground(handlerUrl + "/img/background/sonic.jpg");
			checkFirstTime();
			activeWindow = Windows.SONIC;
		}
	}

	private void setEnding(Windows windowId, boolean recordOnRepository) {
		chromaFilter.unsetBackground();
		pointerDetectorAdvFilter.clearWindows();
		pointerDetectorAdvFilter.addWindow(cpbWindows.fiware);
		faceOverlayFilter.unsetOverlayedImage();
		pointerDetectorAdvFilter.addWindow(cpbWindows.start);
		activeWindow = Windows.START;
		recorderEndpoint.release();
		if (windowId.equals(Windows.YOUTUBE)) {
			String recordUrl = handlerUrl
					+ (recordOnRepository ? "/playerRepository/"
							: "/cpbPlayer/") + itemId;
			getLogger().info("recordUrl " + recordUrl);
			Videos.upload(
					recordUrl,
					PLAYLIST_TOKEN,
					newArrayList("FI-WARE", "Kurento", "FUN-LAB", "GSyC",
							"URJC", "Campus Party", "WebRTC",
							"Software Engineering", "Augmented Reality",
							"Computer Vision", "Super Mario", "Sonic",
							"Street Fighter", "Donkey Kong"));
		}
	}

}

class CpbWindows {

	// Enumeration of windows
	enum Windows {
		START, SF, DK, MARIO, SONIC, TRASH, YOUTUBE, FIWARE
	}

	// Windows instances
	public PointerDetectorWindowMediaParam start;
	public PointerDetectorWindowMediaParam sf;
	public PointerDetectorWindowMediaParam mario;
	public PointerDetectorWindowMediaParam dk;
	public PointerDetectorWindowMediaParam sonic;
	public PointerDetectorWindowMediaParam trash;
	public PointerDetectorWindowMediaParam youtube;
	public PointerDetectorWindowMediaParam fiware;

	public CpbWindows(String handlerUrl) throws URISyntaxException {
		start = new PointerDetectorWindowMediaParam(Windows.START.toString(),
				100, 100, 280, 380);
		start.setImage(handlerUrl + "/img/buttons/start.png");

		mario = new PointerDetectorWindowMediaParam(Windows.MARIO.toString(),
				100, 100, 540, 0);
		mario.setImage(handlerUrl + "/img/buttons/mario.png");

		dk = new PointerDetectorWindowMediaParam(Windows.DK.toString(), 100,
				100, 540, 126);
		dk.setImage(handlerUrl + "/img/buttons/dk.png");

		sf = new PointerDetectorWindowMediaParam(Windows.SF.toString(), 100,
				100, 540, 252);
		sf.setImage(handlerUrl + "/img/buttons/sf.png");

		sonic = new PointerDetectorWindowMediaParam(Windows.SONIC.toString(),
				100, 100, 540, 380);
		sonic.setImage(handlerUrl + "/img/buttons/sonic.png");

		youtube = new PointerDetectorWindowMediaParam(
				Windows.YOUTUBE.toString(), 100, 100, 0, 380);
		youtube.setImage(handlerUrl + "/img/buttons/youtube.png");

		trash = new PointerDetectorWindowMediaParam(Windows.TRASH.toString(),
				100, 100, 0, 190);
		trash.setImage(handlerUrl + "/img/buttons/trash.png");

		fiware = new PointerDetectorWindowMediaParam(Windows.FIWARE.toString(),
				40, 180, 230, 0);
		fiware.setImage(handlerUrl + "/img/buttons/fiware2.png");
	}
}
