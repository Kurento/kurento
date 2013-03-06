package com.kurento.kms.media;

import java.io.IOException;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.kurento.kms.api.MediaType;
import com.kurento.kms.media.internal.KmsConstants;

public class SyncMediaServerTest {

	private static MediaFactory mediaFactory;

	@BeforeClass
	public static void setUpBeforeClass() throws MediaException {
		Properties properties = new Properties();
		properties.setProperty(KmsConstants.SERVER_ADDRESS, "localhost");
		properties.setProperty(KmsConstants.SERVER_PORT, ""
				+ KmsConstants.DEFAULT_SERVER_PORT);

		MediaFactory.init(properties);
		mediaFactory = MediaFactory.getMediaFactory();
	}

	@AfterClass
	public static void afterClass() throws IOException {
		mediaFactory.release();
	}

	@Test
	public void testStreamSync() throws MediaException, IOException,
			InterruptedException {
		Stream stream = mediaFactory.getStream();
		System.out.println("generateOffer sessionDecriptor: "
				+ stream.generateOffer());
		System.out.println("processOffer sessionDecriptor: "
				+ stream.processOffer("processOffer test"));
		System.out.println("processAnswer sessionDecriptor: "
				+ stream.processAnswer("processAnswer test"));
		stream.release();
	}

	@Test
	public void testPlayer() throws MediaException, IOException {
		MediaPlayer player = mediaFactory.getMediaPlayer("");
		player.play();
		player.pause();
		player.stop();
		player.release();
	}

	@Test
	public void testRecorder() throws MediaException, IOException {
		MediaRecorder recorder = mediaFactory.getMediaRecorder("");
		recorder.record();
		recorder.pause();
		recorder.stop();
		recorder.release();
	}

	@Test
	public void testJoinable() throws MediaException, IOException {
		Stream streamA = mediaFactory.getStream();
		Stream streamB = mediaFactory.getStream();

		streamA.join(streamB);
		streamA.unjoin(streamB);

		System.out.println("MediaSrcs: " + streamA.getMediaSrcs());
		System.out.println("MediaSinks: " + streamA.getMediaSinks());

		System.out.println("MediaSrcs audio: "
				+ streamA.getMediaSrcs(MediaType.AUDIO));
		System.out.println("MediaSrcs video: "
				+ streamA.getMediaSrcs(MediaType.VIDEO));

		System.out.println("MediaSinks audio: "
				+ streamA.getMediaSinks(MediaType.AUDIO));
		System.out.println("MediaSinks video: "
				+ streamA.getMediaSinks(MediaType.VIDEO));

		streamA.release();
		streamB.release();
	}

	@Test
	public void testMixer() throws MediaException, IOException,
			InterruptedException {
		DummyMixer mixer = mediaFactory.getMixer(DummyMixer.class);
		mixer.release();
	}

}
