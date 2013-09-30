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

//
//import java.io.IOException;
//import java.util.Collection;
//import java.util.concurrent.Semaphore;
//import java.util.concurrent.TimeUnit;
//
//import org.junit.After;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Ignore;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//
//import com.kurento.kmf.media.objects.MediaPipeline;
//
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration("/kmf-api-test-context.xml")
//public class AsyncMediaServerTest {
//
//	@Autowired
//	private MediaPipelineFactory mediaPipelineFactory;
//
//	private MediaPipeline mediaPipeline;
//
//	@Before
//	public void setUpBeforeClass() throws zMediaException, IOException,
//			InterruptedException {
//		final Semaphore sem = new Semaphore(0);
//		mediaPipelineFactory
//				.createMediaPipeline(new Continuation<zMediaPipeline>() {
//					@Override
//					public void onSuccess(zMediaPipeline result) {
//						System.out.println("getMediaFactory onSuccess");
//						mediaPipeline = result;
//						sem.release();
//					}
//
//					@Override
//					public void onError(Throwable cause) {
//						System.out.println("getMediaFactory onError");
//					}
//				});
//		Assert.assertTrue(sem.tryAcquire(500, TimeUnit.MILLISECONDS));
//	}
//
//	@After
//	public void afterClass() throws IOException, InterruptedException {
//		releaseMediaObject(mediaPipeline);
//	}
//
//	@Test
//	public void testStream() throws zMediaException, IOException,
//			InterruptedException {
//		final Semaphore sem = new Semaphore(0);
//
//		mediaPipeline.createSdpEndPoint(zRtpEndPoint.class,
//				new Continuation<zRtpEndPoint>() {
//					@Override
//					public void onSuccess(zRtpEndPoint result) {
//						System.out.println("getStream onSuccess");
//						zSdpEndPoint stream = result;
//						final Semaphore semCont = new Semaphore(0);
//
//						try {
//							stream.generateOffer(new Continuation<String>() {
//								@Override
//								public void onSuccess(String result) {
//									System.out
//											.println("generateOffer onSuccess. SessionDecriptor: "
//													+ result);
//									semCont.release();
//								}
//
//								@Override
//								public void onError(Throwable cause) {
//									System.out.println("generateOffer onError");
//								}
//							});
//							Assert.assertTrue(semCont.tryAcquire(500,
//									TimeUnit.MILLISECONDS));
//							releaseMediaObject(stream);
//							sem.release();
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//							Assert.fail(e.getMessage());
//						} catch (IOException e) {
//							e.printStackTrace();
//							Assert.fail(e.getMessage());
//						}
//
//						try {
//							stream.processOffer("processOffer test",
//									new Continuation<String>() {
//										@Override
//										public void onSuccess(String result) {
//											System.out
//													.println("processOffer onSuccess. SessionDecriptor: "
//															+ result);
//											semCont.release();
//										}
//
//										@Override
//										public void onError(Throwable cause) {
//											System.out
//													.println("processOffer onError");
//										}
//									});
//							Assert.assertTrue(semCont.tryAcquire(500,
//									TimeUnit.MILLISECONDS));
//							releaseMediaObject(stream);
//							sem.release();
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//							Assert.fail(e.getMessage());
//						} catch (IOException e) {
//							e.printStackTrace();
//							Assert.fail(e.getMessage());
//						}
//
//						try {
//							stream.processAnswer("processAnswer test",
//									new Continuation<String>() {
//										@Override
//										public void onSuccess(String result) {
//											System.out
//													.println("processAnswer onSuccess. SessionDecriptor: "
//															+ result);
//											semCont.release();
//										}
//
//										@Override
//										public void onError(Throwable cause) {
//											System.out
//													.println("processAnswer onError");
//										}
//									});
//							Assert.assertTrue(semCont.tryAcquire(500,
//									TimeUnit.MILLISECONDS));
//							releaseMediaObject(stream);
//							sem.release();
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//							Assert.fail(e.getMessage());
//						} catch (IOException e) {
//							e.printStackTrace();
//							Assert.fail(e.getMessage());
//						}
//
//						try {
//							stream.getLocalSessionDescriptor(new Continuation<String>() {
//								@Override
//								public void onSuccess(String result) {
//									System.out
//											.println("getLocalSessionDescriptor onSuccess. SessionDecriptor: "
//													+ result);
//									semCont.release();
//								}
//
//								@Override
//								public void onError(Throwable cause) {
//									System.out
//											.println("getLocalSessionDescriptor onError");
//								}
//							});
//							Assert.assertTrue(semCont.tryAcquire(500,
//									TimeUnit.MILLISECONDS));
//							releaseMediaObject(stream);
//							sem.release();
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//							Assert.fail(e.getMessage());
//						} catch (IOException e) {
//							e.printStackTrace();
//							Assert.fail(e.getMessage());
//						}
//
//						try {
//							stream.getRemoteSessionDescriptor(new Continuation<String>() {
//								@Override
//								public void onSuccess(String result) {
//									System.out
//											.println("getRemoteSessionDescriptor onSuccess. SessionDecriptor: "
//													+ result);
//									semCont.release();
//								}
//
//								@Override
//								public void onError(Throwable cause) {
//									System.out
//											.println("getRemoteSessionDescriptor onError");
//								}
//							});
//							Assert.assertTrue(semCont.tryAcquire(500,
//									TimeUnit.MILLISECONDS));
//							releaseMediaObject(stream);
//							sem.release();
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//							Assert.fail(e.getMessage());
//						} catch (IOException e) {
//							e.printStackTrace();
//							Assert.fail(e.getMessage());
//						}
//					}
//
//					@Override
//					public void onError(Throwable cause) {
//						System.out.println("getStream onError");
//					}
//				});
//
//		Assert.assertTrue(sem.tryAcquire(500, TimeUnit.MILLISECONDS));
//	}
//
//	// TODO: Enable this test when uri endpoint is implemented
//	@Ignore
//	@Test
//	public void testPlayer() throws zMediaException, IOException,
//			InterruptedException {
//		final Semaphore sem = new Semaphore(0);
//
//		mediaPipeline.createUriEndPoint(zPlayerEndPoint.class, "",
//				new Continuation<zPlayerEndPoint>() {
//					@Override
//					public void onSuccess(zPlayerEndPoint result) {
//						System.out.println("getMediaPlayer onSuccess");
//						zPlayerEndPoint player = result;
//						final Semaphore semCont = new Semaphore(0);
//
//						try {
//							player.play(new Continuation<Void>() {
//								@Override
//								public void onSuccess(Void result) {
//									System.out.println("play onSuccess");
//									semCont.release();
//								}
//
//								@Override
//								public void onError(Throwable cause) {
//									System.out.println("play onError");
//								}
//							});
//							Assert.assertTrue(semCont.tryAcquire(500,
//									TimeUnit.MILLISECONDS));
//
//							player.pause(new Continuation<Void>() {
//								@Override
//								public void onSuccess(Void result) {
//									System.out
//											.println("pause player onSuccess");
//									semCont.release();
//								}
//
//								@Override
//								public void onError(Throwable cause) {
//									System.out.println("pause player onError");
//								}
//							});
//							Assert.assertTrue(semCont.tryAcquire(500,
//									TimeUnit.MILLISECONDS));
//
//							player.stop(new Continuation<Void>() {
//								@Override
//								public void onSuccess(Void result) {
//									System.out.println("stop player onSuccess");
//									semCont.release();
//								}
//
//								@Override
//								public void onError(Throwable cause) {
//									System.out.println("stop player onError");
//								}
//							});
//							Assert.assertTrue(semCont.tryAcquire(500,
//									TimeUnit.MILLISECONDS));
//
//							releaseMediaObject(player);
//							sem.release();
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//							Assert.fail(e.getMessage());
//						} catch (IOException e) {
//							e.printStackTrace();
//							Assert.fail(e.getMessage());
//						}
//					}
//
//					@Override
//					public void onError(Throwable cause) {
//						System.out.println("getMediaPlayer onError");
//					}
//				});
//
//		Assert.assertTrue(sem.tryAcquire(500, TimeUnit.MILLISECONDS));
//	}
//
//	// TODO: Enable this test when uri endpoint is implemented
//	@Ignore
//	@Test
//	public void testRecorder() throws zMediaException, IOException,
//			InterruptedException {
//		final Semaphore sem = new Semaphore(0);
//
//		mediaPipeline.createUriEndPoint(zRecorderEndPoint.class, "",
//				new Continuation<zRecorderEndPoint>() {
//					@Override
//					public void onSuccess(zRecorderEndPoint result) {
//						System.out.println("getMediaRecorder onSuccess");
//						zRecorderEndPoint recorder = result;
//						final Semaphore semCont = new Semaphore(0);
//
//						try {
//							recorder.record(new Continuation<Void>() {
//								@Override
//								public void onSuccess(Void result) {
//									System.out.println("record onSuccess");
//									semCont.release();
//								}
//
//								@Override
//								public void onError(Throwable cause) {
//									System.out.println("record onError");
//								}
//							});
//							Assert.assertTrue(semCont.tryAcquire(500,
//									TimeUnit.MILLISECONDS));
//
//							recorder.pause(new Continuation<Void>() {
//								@Override
//								public void onSuccess(Void result) {
//									System.out
//											.println("pause recorder onSuccess");
//									semCont.release();
//								}
//
//								@Override
//								public void onError(Throwable cause) {
//									System.out
//											.println("pause recorder onError");
//								}
//							});
//							Assert.assertTrue(semCont.tryAcquire(500,
//									TimeUnit.MILLISECONDS));
//
//							recorder.stop(new Continuation<Void>() {
//								@Override
//								public void onSuccess(Void result) {
//									System.out
//											.println("stop recorder onSuccess");
//									semCont.release();
//								}
//
//								@Override
//								public void onError(Throwable cause) {
//									System.out.println("stop recorder onError");
//								}
//							});
//							Assert.assertTrue(semCont.tryAcquire(500,
//									TimeUnit.MILLISECONDS));
//
//							releaseMediaObject(recorder);
//							sem.release();
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//							Assert.fail(e.getMessage());
//						} catch (IOException e) {
//							e.printStackTrace();
//							Assert.fail(e.getMessage());
//						}
//					}
//
//					@Override
//					public void onError(Throwable cause) {
//						System.out.println("getMediaRecorder onError");
//					}
//				});
//
//		Assert.assertTrue(sem.tryAcquire(500, TimeUnit.MILLISECONDS));
//	}
//
//	@Test
//	public void testJoinable() throws zMediaException, IOException,
//			InterruptedException {
//		final Semaphore sem = new Semaphore(0);
//
//		mediaPipeline.createSdpEndPoint(zRtpEndPoint.class,
//				new Continuation<zRtpEndPoint>() {
//					@Override
//					public void onSuccess(zRtpEndPoint result) {
//						System.out.println("getStream A onSuccess");
//						final zSdpEndPoint streamA = result;
//						final Semaphore semContA = new Semaphore(0);
//
//						try {
//							mediaPipeline.createSdpEndPoint(zRtpEndPoint.class,
//									new Continuation<zRtpEndPoint>() {
//										@Override
//										public void onSuccess(zRtpEndPoint result) {
//											System.out
//													.println("getStream A onSuccess");
//											zSdpEndPoint streamB = result;
//											final Semaphore semContB = new Semaphore(
//													0);
//
//											try {
//												streamA.getMediaSrcs(new Continuation<Collection<zMediaSrc>>() {
//													@Override
//													public void onSuccess(
//															Collection<zMediaSrc> result) {
//														System.out
//																.println("getMediaSrcs onSuccess. MediaSrcs: "
//																		+ result);
//														semContB.release();
//													}
//
//													@Override
//													public void onError(
//															Throwable cause) {
//														System.out
//																.println("getMediaSrcs onError");
//													}
//												});
//												Assert.assertTrue(semContB
//														.tryAcquire(
//																500,
//																TimeUnit.MILLISECONDS));
//
//												releaseMediaObject(streamB);
//												semContA.release();
//											} catch (InterruptedException e) {
//												e.printStackTrace();
//												Assert.fail(e.getMessage());
//											} catch (IOException e) {
//												e.printStackTrace();
//												Assert.fail(e.getMessage());
//											}
//										}
//
//										@Override
//										public void onError(Throwable cause) {
//											System.out
//													.println("getStream A onError");
//										}
//									});
//							Assert.assertTrue(semContA.tryAcquire(500,
//									TimeUnit.MILLISECONDS));
//
//							releaseMediaObject(streamA);
//							sem.release();
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//							Assert.fail(e.getMessage());
//						} catch (IOException e) {
//							e.printStackTrace();
//							Assert.fail(e.getMessage());
//						}
//					}
//
//					@Override
//					public void onError(Throwable cause) {
//						System.out.println("getStream A onError");
//					}
//				});
//
//		Assert.assertTrue(sem.tryAcquire(500, TimeUnit.MILLISECONDS));
//	}
//
//	@Test
//	public void testMixer() throws zMediaException, IOException,
//			InterruptedException {
//		final Semaphore sem = new Semaphore(0);
//
//		mediaPipeline.createMixer(zMainMixer.class,
//				new Continuation<zMainMixer>() {
//					@Override
//					public void onSuccess(zMainMixer result) {
//						System.out.println("getMixer onSuccess");
//						try {
//							releaseMediaObject(result);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//							Assert.fail(e.getMessage());
//						} catch (IOException e) {
//							e.printStackTrace();
//							Assert.fail(e.getMessage());
//						}
//						sem.release();
//					}
//
//					@Override
//					public void onError(Throwable cause) {
//						System.out.println("getMixer onError: "
//								+ cause.getMessage());
//					}
//				});
//
//		Assert.assertTrue(sem.tryAcquire(500, TimeUnit.MILLISECONDS));
//	}
//
//	private static void releaseMediaObject(final zMediaObject mo)
//			throws IOException, InterruptedException {
//		final Semaphore sem = new Semaphore(0);
//
//		mo.release(new Continuation<Void>() {
//			@Override
//			public void onSuccess(Void result) {
//				System.out.println(mo.getClass() + ".release() onSuccess");
//				sem.release();
//			}
//
//			@Override
//			public void onError(Throwable cause) {
//				System.out.println(mo.getClass() + ".release() onError");
//			}
//		});
//		Assert.assertTrue(sem.tryAcquire(500, TimeUnit.MILLISECONDS));
//	}
//
//}
