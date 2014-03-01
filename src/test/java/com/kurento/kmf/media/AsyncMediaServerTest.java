/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
package com.kurento.kmf.media;

import static com.kurento.kmf.media.MediaType.AUDIO;
import static com.kurento.kmf.media.MediaType.VIDEO;
import static com.kurento.kmf.media.SyncMediaServerTest.URL_SMALL;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;

public class AsyncMediaServerTest extends AbstractAsyncBaseTest {

	@Ignore
	@Test
	public void testStream() throws InterruptedException {
		final Semaphore sem = new Semaphore(0);

		pipeline.newRtpEndpoint().buildAsync(new Continuation<RtpEndpoint>() {
			@Override
			public void onSuccess(RtpEndpoint result) {
				RtpEndpoint stream = result;
				final Semaphore semCont = new Semaphore(0);

				try {
					stream.generateOffer(new Continuation<String>() {
						@Override
						public void onSuccess(String result) {
							Assert.assertFalse(result.isEmpty());
							semCont.release();
						}

						@Override
						public void onError(Throwable cause) {
							throw new KurentoMediaFrameworkException();
						}
					});
					Assert.assertTrue(semCont.tryAcquire(5000, MILLISECONDS));
					releaseMediaObject(stream);
					sem.release();
				} catch (InterruptedException e) {
					e.printStackTrace();
					Assert.fail(e.getMessage());
				} catch (KurentoMediaFrameworkException e) {
					e.printStackTrace();
					Assert.fail(e.getMessage());
				}

				try {
					stream.processOffer("processOffer test",
							new Continuation<String>() {
								@Override
								public void onSuccess(String result) {
									Assert.assertFalse(result.isEmpty());
									semCont.release();
								}

								@Override
								public void onError(Throwable cause) {
									throw new KurentoMediaFrameworkException();
								}
							});
					Assert.assertTrue(semCont.tryAcquire(500, MILLISECONDS));
					releaseMediaObject(stream);
					sem.release();
				} catch (InterruptedException e) {
					Assert.fail(e.getMessage());
				} catch (KurentoMediaFrameworkException e) {
					Assert.fail(e.getMessage());
				}

				try {
					stream.processAnswer("processAnswer test",
							new Continuation<String>() {
								@Override
								public void onSuccess(String result) {
									Assert.assertFalse(result.isEmpty());
									semCont.release();
								}

								@Override
								public void onError(Throwable cause) {
									throw new KurentoMediaFrameworkException();
								}
							});
					Assert.assertTrue(semCont.tryAcquire(500, MILLISECONDS));
					releaseMediaObject(stream);
					sem.release();
				} catch (InterruptedException e) {
					Assert.fail(e.getMessage());
				} catch (KurentoMediaFrameworkException e) {
					Assert.fail(e.getMessage());
				}

				try {
					stream.getLocalSessionDescriptor(new Continuation<String>() {
						@Override
						public void onSuccess(String result) {
							System.out
									.println("getLocalSessionDescriptor onSuccess. SessionDecriptor: "
											+ result);
							semCont.release();
						}

						@Override
						public void onError(Throwable cause) {
							throw new KurentoMediaFrameworkException();
						}
					});
					Assert.assertTrue(semCont.tryAcquire(500, MILLISECONDS));
					releaseMediaObject(stream);
					sem.release();
				} catch (InterruptedException e) {
					Assert.fail(e.getMessage());
				} catch (KurentoMediaFrameworkException e) {
					Assert.fail(e.getMessage());
				}

				try {
					stream.getRemoteSessionDescriptor(new Continuation<String>() {
						@Override
						public void onSuccess(String result) {
							System.out
									.println("getRemoteSessionDescriptor onSuccess. SessionDecriptor: "
											+ result);
							semCont.release();
						}

						@Override
						public void onError(Throwable cause) {
							throw new KurentoMediaFrameworkException();
						}
					});
					Assert.assertTrue(semCont.tryAcquire(500, MILLISECONDS));
					releaseMediaObject(stream);
					sem.release();
				} catch (InterruptedException e) {
					Assert.fail(e.getMessage());
				} catch (KurentoMediaFrameworkException e) {
					Assert.fail(e.getMessage());
				}
			}

			@Override
			public void onError(Throwable cause) {
				System.out.println("getStream onError");
			}
		});

		Assert.assertTrue(sem.tryAcquire(50000, MILLISECONDS));
	}

	@Test
	public void testSourceSinks() throws KurentoMediaFrameworkException,
			InterruptedException {
		RtpEndpoint rtp = pipeline.newRtpEndpoint().build();

		final BlockingQueue<Collection<MediaSink>> sinkEvent = new ArrayBlockingQueue<Collection<MediaSink>>(
				1);
		final BlockingQueue<Collection<MediaSource>> srcEvent = new ArrayBlockingQueue<Collection<MediaSource>>(
				1);
		rtp.getMediaSrcs(VIDEO, new Continuation<List<MediaSource>>() {

			@Override
			public void onSuccess(List<MediaSource> result) {
				srcEvent.add(result);
			}

			@Override
			public void onError(Throwable cause) {
				throw new KurentoMediaFrameworkException(cause);
			}
		});

		Assert.assertNotNull(srcEvent.poll(500, MILLISECONDS));

		rtp.getMediaSinks(MediaType.VIDEO, new Continuation<List<MediaSink>>() {

			@Override
			public void onSuccess(List<MediaSink> result) {
				sinkEvent.add(result);
			}

			@Override
			public void onError(Throwable cause) {
				throw new KurentoMediaFrameworkException(cause);
			}
		});
		Assert.assertNotNull(sinkEvent.poll(500, MILLISECONDS));

		rtp.getMediaSrcs(AUDIO, new Continuation<List<MediaSource>>() {

			@Override
			public void onSuccess(List<MediaSource> result) {
				srcEvent.add(result);
			}

			@Override
			public void onError(Throwable cause) {
				throw new KurentoMediaFrameworkException(cause);
			}
		});
		Assert.assertNotNull(srcEvent.poll(500, MILLISECONDS));

		rtp.getMediaSinks(AUDIO, new Continuation<List<MediaSink>>() {

			@Override
			public void onSuccess(List<MediaSink> result) {
				sinkEvent.add(result);
			}

			@Override
			public void onError(Throwable cause) {
				throw new KurentoMediaFrameworkException(cause);
			}
		});
		Assert.assertNotNull(sinkEvent.poll(500, MILLISECONDS));

		rtp.release();
	}

	@Test
	public void testConnect() throws InterruptedException {
		PlayerEndpoint player = pipeline.newPlayerEndpoint(URL_SMALL).build();
		HttpEndpoint http = pipeline.newHttpGetEndpoint().build();

		final CountDownLatch latch = new CountDownLatch(1);
		player.connect(http, new Continuation<Void>() {

			@Override
			public void onSuccess(Void result) {
				latch.countDown();
			}

			@Override
			public void onError(Throwable cause) {
				throw new KurentoMediaFrameworkException(cause);
			}
		});

		Assert.assertTrue(latch.await(500, MILLISECONDS));

		player.play();
		http.release();
		player.release();
	}

	@Test
	public void testConnectByType() throws InterruptedException {
		PlayerEndpoint player = pipeline.newPlayerEndpoint(URL_SMALL).build();
		HttpEndpoint http = pipeline.newHttpGetEndpoint().build();

		final CountDownLatch audioLatch = new CountDownLatch(1);
		player.connect(http, AUDIO, new Continuation<Void>() {

			@Override
			public void onSuccess(Void result) {
				audioLatch.countDown();
			}

			@Override
			public void onError(Throwable cause) {
				throw new KurentoMediaFrameworkException(cause);
			}
		});

		Assert.assertTrue(audioLatch.await(500, MILLISECONDS));

		final CountDownLatch videoLatch = new CountDownLatch(1);
		player.connect(http, VIDEO, new Continuation<Void>() {

			@Override
			public void onSuccess(Void result) {
				videoLatch.countDown();
			}

			@Override
			public void onError(Throwable cause) {
				throw new KurentoMediaFrameworkException(cause);
			}
		});

		Assert.assertTrue(videoLatch.await(500, MILLISECONDS));

		player.play();
		http.release();
		player.release();
	}

}
