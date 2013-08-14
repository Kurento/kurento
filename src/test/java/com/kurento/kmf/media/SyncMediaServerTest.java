package com.kurento.kmf.media;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.kms.api.MediaType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/kmf-api-test-context.xml")
public class SyncMediaServerTest {

	private static final Logger log = LoggerFactory
			.getLogger(SyncMediaServerTest.class);

	@Autowired
	@Qualifier("mediaManagerFactory")
	private MediaManagerFactory mediaManagerFactory;

	private MediaManager mediaManager;

	@Before
	public void setUpBeforeClass() throws MediaException, IOException {
		mediaManager = mediaManagerFactory.createMediaManager();
	}

	@After
	public void afterClass() throws IOException {
		mediaManager.release();
	}

	@Test
	public void testStreamSync() throws MediaException, IOException,
			InterruptedException {
		RtpEndPoint stream = mediaManager.createSdpEndPoint(RtpEndPoint.class);
		log.debug("generateOffer sessionDecriptor: " + stream.generateOffer());
		log.debug("processOffer sessionDecriptor: "
				+ stream.processOffer("processOffer test"));
		log.debug("processAnswer sessionDecriptor: "
				+ stream.processAnswer("processAnswer test"));
		stream.release();
	}

	// TODO: Enable this test when uri endpoint is implemented
	@Ignore
	@Test
	public void testPlayer() throws MediaException, IOException {
		PlayerEndPoint player = mediaManager.createUriEndPoint(
				PlayerEndPoint.class, "");
		player.play();
		player.pause();
		player.stop();
		player.release();
	}

	// TODO: Enable this test when uri endpoint is implemented
	@Ignore
	@Test
	public void testRecorder() throws MediaException, IOException {
		RecorderEndPoint recorder = mediaManager.createUriEndPoint(
				RecorderEndPoint.class, "");
		recorder.record();
		recorder.pause();
		recorder.stop();
		recorder.release();
	}

	@Test
	public void testJoinable() throws MediaException, IOException {
		RtpEndPoint streamA = mediaManager.createSdpEndPoint(RtpEndPoint.class);
		RtpEndPoint streamB = mediaManager.createSdpEndPoint(RtpEndPoint.class);

		log.debug("MediaSrcs: " + streamA.getMediaSrcs());
		log.debug("MediaSinks: " + streamA.getMediaSinks());

		log.debug("MediaSrcs audio: " + streamA.getMediaSrcs(MediaType.AUDIO));
		log.debug("MediaSrcs video: " + streamA.getMediaSrcs(MediaType.VIDEO));

		log.debug("MediaSinks audio: " + streamA.getMediaSinks(MediaType.AUDIO));
		log.debug("MediaSinks video: " + streamA.getMediaSinks(MediaType.VIDEO));

		streamA.release();
		streamB.release();
	}

	@Test
	public void testMixer() throws MediaException, IOException,
			InterruptedException {
		MainMixer mixer = mediaManager.createMixer(MainMixer.class);
		mixer.release();
	}

}
