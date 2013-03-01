package com.kurento.kms.media;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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

		mediaFactory = new MediaFactory(properties);
	}

	@AfterClass
	public static void afterClass() {
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
	}

	@Test
	public void testPlayer() throws MediaException, IOException {
		MediaPlayer player = mediaFactory.getMediaPlayer("");
		player.play();
	}
}
