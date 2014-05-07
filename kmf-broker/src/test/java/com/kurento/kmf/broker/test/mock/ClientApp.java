package com.kurento.kmf.broker.test.mock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import kmf.broker.Broker;
import kmf.broker.client.JsonRpcClientBroker;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.test.HttpServer;
import com.kurento.kmf.test.PortManager;
import com.kurento.kmf.test.client.Browser;
import com.kurento.kmf.test.client.BrowserClient;
import com.kurento.kmf.test.client.Client;
import com.kurento.kmf.test.client.EventListener;

public class ClientApp {
	
	public final static int TIMEOUT = 60;
	
	private static Logger LOG = LoggerFactory.getLogger(ClientApp.class);
	
	private volatile static HttpServer server;
	
	static {
		try {
			server = new HttpServer(PortManager.getPort());
			server.start();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	private MediaPipelineFactory mpf;
	private Broker broker;

	private CountDownLatch finished = new CountDownLatch(1);
		
	public ClientApp(String logId) {
		this.broker = new Broker(logId);
		this.mpf = new MediaPipelineFactory(new JsonRpcClientBroker(broker));
	}

	public void start() {

		this.broker.init();
		
		new Thread() {
			@Override
			public void run() {
				try {
					useMediaAPI();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	private void useMediaAPI() throws InterruptedException {
		
		MediaPipeline mp = mpf.create();
		PlayerEndpoint playerEP = mp.newPlayerEndpoint(
				"http://ci.kurento.com/video/small.webm").build();
		HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();
		playerEP.connect(httpEP);
		String url = httpEP.getUrl();
		LOG.info("url: {}", url);

		final CountDownLatch endOfStreamEvent = new CountDownLatch(1);
		playerEP.addEndOfStreamListener(new MediaEventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				endOfStreamEvent.countDown();
			}
		});

		// Test execution
		final CountDownLatch startEvent = new CountDownLatch(1);
		final CountDownLatch terminationEvent = new CountDownLatch(1);

		try (BrowserClient browser = new BrowserClient(PortManager.getPort(),
				Browser.CHROME, Client.PLAYER)) {
			browser.setURL(url);
			browser.addEventListener("playing", new EventListener() {
				@Override
				public void onEvent(String event) {
					LOG.info("*** playing ***");
					startEvent.countDown();
				}
			});
			browser.addEventListener("ended", new EventListener() {
				@Override
				public void onEvent(String event) {
					LOG.info("*** ended ***");
					terminationEvent.countDown();
				}
			});
			playerEP.play();
			browser.start();

			Assert.assertTrue(startEvent.await(TIMEOUT, TimeUnit.SECONDS));
			long startTime = System.currentTimeMillis();
			Assert.assertTrue(terminationEvent.await(TIMEOUT, TimeUnit.SECONDS));
			long duration = System.currentTimeMillis() - startTime;
			LOG.info("Video duration: " + (duration / 60) + " seconds");
		}

		Assert.assertTrue("The player should fire 'EndOfStreamEvent'",
				endOfStreamEvent.await(TIMEOUT, TimeUnit.SECONDS));

		playerEP.release();
		httpEP.release();
		mp.release();
		
		finished.countDown();
	}

	public void await() throws InterruptedException {
		finished.await(TIMEOUT, TimeUnit.SECONDS);		
	}
}
