package com.kurento.tutorial;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.kurento.kmf.media.FaceOverlayFilter;
import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.factory.KmfMediaApi;
import com.kurento.kmf.media.factory.MediaPipelineFactory;

public class HelloWorldKurento {

	public static void main(String[] args) throws InterruptedException,
			IOException, URISyntaxException {

		MediaPipelineFactory kurento = KmfMediaApi
				.createMediaPipelineFactoryFromSystemProps();

		MediaPipeline pipeline = kurento.create();

		PlayerEndpoint player = pipeline.newPlayerEndpoint(
				"http://files.kurento.org/video/fiwarecut.mp4").build();
		// player.addEndOfStreamListener(new
		// MediaEventListener<EndOfStreamEvent>() {
		//
		// @Override
		// public void onEvent(EndOfStreamEvent event) {
		// System.out.println("Finish");
		// System.exit(0);
		// }
		// });

		FaceOverlayFilter filter = pipeline.newFaceOverlayFilter().build();
		filter.setOverlayedImage(
				"http://files.kurento.org/imgs/mario-wings.png", -0.35F, -1.2F,
				1.6F, 1.6F);

		HttpGetEndpoint recorder = pipeline.newHttpGetEndpoint().build();

		// RecorderEndpoint recorder = pipeline.newRecorderEndpoint(
		// "file:///home/mica/Data/Kurento/tmp/video.mp4").build();

		player.connect(filter);
		filter.connect(recorder);

		player.play();

		Desktop.getDesktop().browse(new URI(recorder.getUrl()));

	}

}
