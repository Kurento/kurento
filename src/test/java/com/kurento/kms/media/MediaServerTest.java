package com.kurento.kms.media;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.kurento.kms.api.MediaType;
import com.kurento.kms.media.Stream.Continuation;
import com.kurento.kms.media.internal.KmsConstants;

public class MediaServerTest {

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
	public void testStream() throws MediaException, IOException,
			InterruptedException {
		Stream stream = mediaFactory.getStream();
		final Semaphore sem = new Semaphore(0);
		stream.generateOffer(new Continuation() {
			@Override
			public void onSucess(String spec) {
				System.out.println("onSucess. spec: " + spec);
				sem.release();
			}

			@Override
			public void onError(Throwable cause) {
				System.out.println("onError");
			}
		});

		Assert.assertTrue(sem.tryAcquire(10, TimeUnit.SECONDS));
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

}
