package com.kurento.kmf.media;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.Assert;
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
	@Qualifier("mediaPipelineFactory")
	private MediaPipelineFactory mediaPipelineFactory;

	private MediaPipeline mediaPipeline;

	@Before
	public void setUpBeforeClass() throws MediaException, IOException {
		mediaPipeline = mediaPipelineFactory.createMediaPipeline();
	}

	@After
	public void afterClass() throws IOException {
		mediaPipeline.release();
	}

	@Test
	public void testStreamSync() throws MediaException, IOException,
			InterruptedException {
		RtpEndPoint stream = mediaPipeline.createSdpEndPoint(RtpEndPoint.class);
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
		PlayerEndPoint player = mediaPipeline.createUriEndPoint(
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
		RecorderEndPoint recorder = mediaPipeline.createUriEndPoint(
				RecorderEndPoint.class, "");
		recorder.record();
		recorder.pause();
		recorder.stop();
		recorder.release();
	}

	@Test
	public void testJoinable() throws MediaException, IOException {
		RtpEndPoint streamA = mediaPipeline
				.createSdpEndPoint(RtpEndPoint.class);
		RtpEndPoint streamB = mediaPipeline
				.createSdpEndPoint(RtpEndPoint.class);

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
		MainMixer mixer = mediaPipeline.createMixer(MainMixer.class);
		mixer.release();
	}

	@Test
	public void testZBar() throws IOException, MediaException,
			InterruptedException {
		PlayerEndPoint player = mediaPipeline.createUriEndPoint(
				PlayerEndPoint.class,
				"https://ci.kurento.com/video/barcodes.webm");
		ZBarFilter zbar = mediaPipeline.createFilter(ZBarFilter.class);

		MediaSink videoSink = zbar.getMediaSinks(MediaType.VIDEO).iterator()
				.next();
		MediaSrc videoSrc = player.getMediaSrcs(MediaType.VIDEO).iterator()
				.next();

		videoSrc.connect(videoSink);

		final Semaphore sem = new Semaphore(0);

		zbar.addListener(new MediaEventListener<ZBarEvent>() {

			@Override
			public void onEvent(ZBarEvent event) {
				log.info("ZBar event received:\n" + event);
				sem.release();
			}
		});

		player.play();

		Assert.assertTrue(sem.tryAcquire(10, TimeUnit.SECONDS));

		player.stop();
		zbar.release();
		player.release();
	}

	@Test
	public void testHttpEndPoint() throws IOException, MediaException,
			InterruptedException {
		final PlayerEndPoint player = mediaPipeline
				.createUriEndPoint(PlayerEndPoint.class,
						"https://ci.kurento.com/video/small.webm");
		HttpEndPoint httpEndPoint = mediaPipeline.createHttpEndPoint();

		MediaSink videoSink = httpEndPoint.getMediaSinks(MediaType.VIDEO)
				.iterator().next();
		MediaSrc videoSrc = player.getMediaSrcs(MediaType.VIDEO).iterator()
				.next();
		videoSrc.connect(videoSink);

		final Semaphore sem = new Semaphore(0);

		player.addListener(new MediaEventListener<PlayerEvent>() {

			@Override
			public void onEvent(PlayerEvent event) {
				sem.release();
			}
		});

		httpEndPoint.addListener(new MediaEventListener<HttpEndPointEvent>() {

			@Override
			public void onEvent(HttpEndPointEvent event) {
				log.info("received: " + event);
				try {
					player.play();
				} catch (IOException e) {
					e.printStackTrace();
					Assert.fail();
				}
			}
		});

		log.info("Url: -- " + httpEndPoint.getUrl());
		DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.execute(new HttpGet(httpEndPoint.getUrl()));

		// TODO Change this by a try acquire when test is automated
		sem.acquire();

		player.release();
		httpEndPoint.release();
	}
}
