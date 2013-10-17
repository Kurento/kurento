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

import java.util.Collection;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/kmf-api-test-context.xml")
public class AsyncMediaServerTest {

	@Autowired
	private MediaPipelineFactory mediaPipelineFactory;

	private MediaPipeline mediaPipeline;

	@Before
	public void setUpBeforeClass() throws InterruptedException {
		final Semaphore sem = new Semaphore(0);
		mediaPipelineFactory.create(new Continuation<MediaPipeline>() {
			@Override
			public void onSuccess(MediaPipeline result) {
				System.out.println("getMediaFactory onSuccess");
				mediaPipeline = result;
				sem.release();
			}

			@Override
			public void onError(Throwable cause) {
				System.out.println("getMediaFactory onError");
			}
		});
		Assert.assertTrue(sem.tryAcquire(500, TimeUnit.MILLISECONDS));
	}

	@After
	public void afterClass() throws InterruptedException {
		releaseMediaObject(mediaPipeline);
	}

	// TODO reinstate once commands are implemented
	@Ignore
	@Test
	public void testStream() throws InterruptedException {
		final Semaphore sem = new Semaphore(0);

		mediaPipeline.createRtpEndPoint(new Continuation<RtpEndPoint>() {
			@Override
			public void onSuccess(RtpEndPoint result) {
				System.out.println("getStream onSuccess");
				RtpEndPoint stream = result;
				final Semaphore semCont = new Semaphore(0);

				try {
					stream.generateOffer(new Continuation<String>() {
						@Override
						public void onSuccess(String result) {
							System.out
									.println("generateOffer onSuccess. SessionDecriptor: "
											+ result);
							semCont.release();
						}

						@Override
						public void onError(Throwable cause) {
							System.out.println("generateOffer onError");
						}
					});
					Assert.assertTrue(semCont.tryAcquire(5000,
							TimeUnit.MILLISECONDS));
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
									System.out
											.println("processOffer onSuccess. SessionDecriptor: "
													+ result);
									semCont.release();
								}

								@Override
								public void onError(Throwable cause) {
									System.out.println("processOffer onError");
								}
							});
					Assert.assertTrue(semCont.tryAcquire(500,
							TimeUnit.MILLISECONDS));
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
					stream.processAnswer("processAnswer test",
							new Continuation<String>() {
								@Override
								public void onSuccess(String result) {
									System.out
											.println("processAnswer onSuccess. SessionDecriptor: "
													+ result);
									semCont.release();
								}

								@Override
								public void onError(Throwable cause) {
									System.out.println("processAnswer onError");
								}
							});
					Assert.assertTrue(semCont.tryAcquire(500,
							TimeUnit.MILLISECONDS));
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
							System.out
									.println("getLocalSessionDescriptor onError");
						}
					});
					Assert.assertTrue(semCont.tryAcquire(500,
							TimeUnit.MILLISECONDS));
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
							System.out
									.println("getRemoteSessionDescriptor onError");
						}
					});
					Assert.assertTrue(semCont.tryAcquire(500,
							TimeUnit.MILLISECONDS));
					releaseMediaObject(stream);
					sem.release();
				} catch (InterruptedException e) {
					e.printStackTrace();
					Assert.fail(e.getMessage());
				} catch (KurentoMediaFrameworkException e) {
					e.printStackTrace();
					Assert.fail(e.getMessage());
				}
			}

			@Override
			public void onError(Throwable cause) {
				System.out.println("getStream onError");
			}
		});

		Assert.assertTrue(sem.tryAcquire(50000, TimeUnit.MILLISECONDS));
	}

	// TODO: Enable this test when uri endpoint is implemented
	@Ignore
	@Test
	public void testPlayer() throws InterruptedException {
		final Semaphore sem = new Semaphore(0);

		mediaPipeline.createPlayerEndPoint("",
				new Continuation<PlayerEndPoint>() {
					@Override
					public void onSuccess(PlayerEndPoint result) {
						System.out.println("getMediaPlayer onSuccess");
						PlayerEndPoint player = result;
						final Semaphore semCont = new Semaphore(0);

						try {
							player.play(new Continuation<Void>() {
								@Override
								public void onSuccess(Void result) {
									System.out.println("play onSuccess");
									semCont.release();
								}

								@Override
								public void onError(Throwable cause) {
									System.out.println("play onError");
								}
							});
							Assert.assertTrue(semCont.tryAcquire(500,
									TimeUnit.MILLISECONDS));

							player.pause(new Continuation<Void>() {
								@Override
								public void onSuccess(Void result) {
									System.out
											.println("pause player onSuccess");
									semCont.release();
								}

								@Override
								public void onError(Throwable cause) {
									System.out.println("pause player onError");
								}
							});
							Assert.assertTrue(semCont.tryAcquire(500,
									TimeUnit.MILLISECONDS));

							player.stop(new Continuation<Void>() {
								@Override
								public void onSuccess(Void result) {
									System.out.println("stop player onSuccess");
									semCont.release();
								}

								@Override
								public void onError(Throwable cause) {
									System.out.println("stop player onError");
								}
							});
							Assert.assertTrue(semCont.tryAcquire(500,
									TimeUnit.MILLISECONDS));

							releaseMediaObject(player);
							sem.release();
						} catch (InterruptedException e) {
							e.printStackTrace();
							Assert.fail(e.getMessage());
						} catch (KurentoMediaFrameworkException e) {
							e.printStackTrace();
							Assert.fail(e.getMessage());
						}
					}

					@Override
					public void onError(Throwable cause) {
						System.out.println("getMediaPlayer onError");
					}
				});

		Assert.assertTrue(sem.tryAcquire(500, TimeUnit.MILLISECONDS));
	}

	// TODO: Enable this test when uri endpoint is implemented
	@Ignore
	@Test
	public void testRecorder() throws InterruptedException {
		final Semaphore sem = new Semaphore(0);

		mediaPipeline.createRecorderEndPoint("",
				new Continuation<RecorderEndPoint>() {
					@Override
					public void onSuccess(RecorderEndPoint result) {
						System.out.println("getMediaRecorder onSuccess");
						RecorderEndPoint recorder = result;
						final Semaphore semCont = new Semaphore(0);

						try {
							recorder.record(new Continuation<Void>() {
								@Override
								public void onSuccess(Void result) {
									System.out.println("record onSuccess");
									semCont.release();
								}

								@Override
								public void onError(Throwable cause) {
									System.out.println("record onError");
								}
							});
							Assert.assertTrue(semCont.tryAcquire(500,
									TimeUnit.MILLISECONDS));

							recorder.pause(new Continuation<Void>() {
								@Override
								public void onSuccess(Void result) {
									System.out
											.println("pause recorder onSuccess");
									semCont.release();
								}

								@Override
								public void onError(Throwable cause) {
									System.out
											.println("pause recorder onError");
								}
							});
							Assert.assertTrue(semCont.tryAcquire(500,
									TimeUnit.MILLISECONDS));

							recorder.stop(new Continuation<Void>() {
								@Override
								public void onSuccess(Void result) {
									System.out
											.println("stop recorder onSuccess");
									semCont.release();
								}

								@Override
								public void onError(Throwable cause) {
									System.out.println("stop recorder onError");
								}
							});
							Assert.assertTrue(semCont.tryAcquire(500,
									TimeUnit.MILLISECONDS));

							releaseMediaObject(recorder);
							sem.release();
						} catch (InterruptedException e) {
							e.printStackTrace();
							Assert.fail(e.getMessage());
						} catch (KurentoMediaFrameworkException e) {
							e.printStackTrace();
							Assert.fail(e.getMessage());
						}
					}

					@Override
					public void onError(Throwable cause) {
						System.out.println("getMediaRecorder onError");
					}
				});

		Assert.assertTrue(sem.tryAcquire(500, TimeUnit.MILLISECONDS));
	}

	@Test
	public void testJoinable() throws InterruptedException {
		final Semaphore sem = new Semaphore(0);

		mediaPipeline.createRtpEndPoint(new Continuation<RtpEndPoint>() {
			@Override
			public void onSuccess(RtpEndPoint result) {
				System.out.println("getStream A onSuccess");
				final SdpEndPoint streamA = result;
				final Semaphore semContA = new Semaphore(0);

				try {
					mediaPipeline
							.createRtpEndPoint(new Continuation<RtpEndPoint>() {
								@Override
								public void onSuccess(RtpEndPoint result) {
									System.out.println("getStream A onSuccess");
									SdpEndPoint streamB = result;
									final Semaphore semContB = new Semaphore(0);

									try {
										streamA.getMediaSrcs(new Continuation<Collection<MediaSource>>() {
											@Override
											public void onSuccess(
													Collection<MediaSource> result) {
												System.out
														.println("getMediaSrcs onSuccess. MediaSrcs: "
																+ result);
												semContB.release();
											}

											@Override
											public void onError(Throwable cause) {
												System.out
														.println("getMediaSrcs onError");
											}
										});
										Assert.assertTrue(semContB.tryAcquire(
												500, TimeUnit.MILLISECONDS));

										releaseMediaObject(streamB);
										semContA.release();
									} catch (InterruptedException e) {
										e.printStackTrace();
										Assert.fail(e.getMessage());
									} catch (KurentoMediaFrameworkException e) {
										e.printStackTrace();
										Assert.fail(e.getMessage());
									}
								}

								@Override
								public void onError(Throwable cause) {
									System.out.println("getStream A onError");
								}
							});
					Assert.assertTrue(semContA.tryAcquire(500,
							TimeUnit.MILLISECONDS));

					releaseMediaObject(streamA);
					sem.release();
				} catch (InterruptedException e) {
					e.printStackTrace();
					Assert.fail(e.getMessage());
				} catch (KurentoMediaFrameworkException e) {
					e.printStackTrace();
					Assert.fail(e.getMessage());
				}
			}

			@Override
			public void onError(Throwable cause) {
				System.out.println("getStream A onError");
			}
		});

		Assert.assertTrue(sem.tryAcquire(500, TimeUnit.MILLISECONDS));
	}

	@Test
	public void testMixer() throws InterruptedException {
		final Semaphore sem = new Semaphore(0);

		mediaPipeline.createMediaMixer("MainMixer",
				new Continuation<MediaMixer>() {
					@Override
					public void onSuccess(MediaMixer result) {
						System.out.println("getMixer onSuccess");
						try {
							releaseMediaObject(result);
						} catch (InterruptedException e) {
							e.printStackTrace();
							Assert.fail(e.getMessage());
						} catch (KurentoMediaFrameworkException e) {
							e.printStackTrace();
							Assert.fail(e.getMessage());
						}
						sem.release();
					}

					@Override
					public void onError(Throwable cause) {
						System.out.println("getMixer onError: "
								+ cause.getMessage());
					}
				});

		Assert.assertTrue(sem.tryAcquire(500, TimeUnit.MILLISECONDS));
	}

	private static void releaseMediaObject(final MediaObject mo)
			throws InterruptedException {
		final Semaphore sem = new Semaphore(0);

		mo.release(new Continuation<Void>() {
			@Override
			public void onSuccess(Void result) {
				System.out.println(mo.getClass() + ".release() onSuccess");
				sem.release();
			}

			@Override
			public void onError(Throwable cause) {
				System.out.println(mo.getClass() + ".release() onError");
			}
		});
		Assert.assertTrue(sem.tryAcquire(500, TimeUnit.MILLISECONDS));
	}

}
