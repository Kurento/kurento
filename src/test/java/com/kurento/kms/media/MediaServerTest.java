package com.kurento.kms.media;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.kurento.kms.media.internal.KmsConstants;

public class MediaServerTest {

	private static MediaFactory mediaFactory;

	@BeforeClass
	public static void setUpBeforeClass() throws MediaException, IOException,
			InterruptedException {
		Properties properties = new Properties();
		properties.setProperty(KmsConstants.SERVER_ADDRESS, "localhost");
		properties.setProperty(KmsConstants.SERVER_PORT, ""
				+ KmsConstants.DEFAULT_SERVER_PORT);

		MediaFactory.init(properties);

		final Semaphore sem = new Semaphore(0);
		MediaFactory.getMediaFactory(new Continuation<MediaFactory>() {
			@Override
			public void onSuccess(MediaFactory result) {
				System.out.println("getMediaFactory onSuccess");
				mediaFactory = result;
				sem.release();
			}

			@Override
			public void onError(Throwable cause) {
				System.out.println("getMediaFactory onError");
			}
		});
		Assert.assertTrue(sem.tryAcquire(500, TimeUnit.MILLISECONDS));
	}

	@AfterClass
	public static void afterClass() throws IOException, InterruptedException {
		releaseMediaObject(mediaFactory);
	}

	@Test
	public void testStream() throws MediaException, IOException,
			InterruptedException {
		final Semaphore sem = new Semaphore(0);

		mediaFactory.getStream(new Continuation<Stream>() {
			@Override
			public void onSuccess(Stream result) {
				System.out.println("getStream onSuccess");
				Stream stream = result;
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
					Assert.assertTrue(semCont.tryAcquire(500,
							TimeUnit.MILLISECONDS));
					releaseMediaObject(stream);
					sem.release();
				} catch (InterruptedException e) {
					e.printStackTrace();
					Assert.fail(e.getMessage());
				} catch (IOException e) {
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
				} catch (IOException e) {
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
				} catch (IOException e) {
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
				} catch (IOException e) {
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
				} catch (IOException e) {
					e.printStackTrace();
					Assert.fail(e.getMessage());
				}
			}

			@Override
			public void onError(Throwable cause) {
				System.out.println("getStream onError");
			}
		});

		Assert.assertTrue(sem.tryAcquire(500, TimeUnit.MILLISECONDS));
	}

	@Test
	public void testPlayer() throws MediaException, IOException,
			InterruptedException {
		final Semaphore sem = new Semaphore(0);

		mediaFactory.getMediaPlayer("", new Continuation<MediaPlayer>() {
			@Override
			public void onSuccess(MediaPlayer result) {
				System.out.println("getMediaPlayer onSuccess");
				MediaPlayer player = result;
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
							System.out.println("pause player onSuccess");
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
				} catch (IOException e) {
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

	@Test
	public void testRecorder() throws MediaException, IOException,
			InterruptedException {
		final Semaphore sem = new Semaphore(0);

		mediaFactory.getMediaRecorder("", new Continuation<MediaRecorder>() {
			@Override
			public void onSuccess(MediaRecorder result) {
				System.out.println("getMediaRecorder onSuccess");
				MediaRecorder recorder = result;
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
							System.out.println("pause recorder onSuccess");
							semCont.release();
						}

						@Override
						public void onError(Throwable cause) {
							System.out.println("pause recorder onError");
						}
					});
					Assert.assertTrue(semCont.tryAcquire(500,
							TimeUnit.MILLISECONDS));

					recorder.stop(new Continuation<Void>() {
						@Override
						public void onSuccess(Void result) {
							System.out.println("stop recorder onSuccess");
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
				} catch (IOException e) {
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
	public void testJoinable() throws MediaException, IOException,
			InterruptedException {
		final Semaphore sem = new Semaphore(0);

		mediaFactory.getStream(new Continuation<Stream>() {
			@Override
			public void onSuccess(Stream result) {
				System.out.println("getStream A onSuccess");
				final Stream streamA = result;
				final Semaphore semContA = new Semaphore(0);

				try {
					mediaFactory.getStream(new Continuation<Stream>() {
						@Override
						public void onSuccess(Stream result) {
							System.out.println("getStream A onSuccess");
							Stream streamB = result;
							final Semaphore semContB = new Semaphore(0);

							try {
								streamA.join(streamB, new Continuation<Void>() {
									@Override
									public void onSuccess(Void result) {
										System.out.println("join onSuccess");
										semContB.release();
									}

									@Override
									public void onError(Throwable cause) {
										System.out.println("join onError");
									}
								});
								Assert.assertTrue(semContB.tryAcquire(500,
										TimeUnit.MILLISECONDS));

								streamA.getMediaSrcs(new Continuation<Collection<MediaSrc>>() {
									@Override
									public void onSuccess(
											Collection<MediaSrc> result) {
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
								Assert.assertTrue(semContB.tryAcquire(500,
										TimeUnit.MILLISECONDS));

								releaseMediaObject(streamB);
								semContA.release();
							} catch (InterruptedException e) {
								e.printStackTrace();
								Assert.fail(e.getMessage());
							} catch (IOException e) {
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
				} catch (IOException e) {
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

		// System.out.println("MediaSinks: " + streamA.getMediaSinks());
		//
		// System.out.println("MediaSrcs audio: "
		// + streamA.getMediaSrcs(MediaType.AUDIO));
		// System.out.println("MediaSrcs video: "
		// + streamA.getMediaSrcs(MediaType.VIDEO));
		//
		// System.out.println("MediaSinks audio: "
		// + streamA.getMediaSinks(MediaType.AUDIO));
		// System.out.println("MediaSinks video: "
		// + streamA.getMediaSinks(MediaType.VIDEO));
	}

	@Test
	public void testMixer() throws MediaException, IOException,
			InterruptedException {
		final Semaphore sem = new Semaphore(0);

		mediaFactory.getMixer(DummyMixer.class, new Continuation<DummyMixer>() {
			@Override
			public void onSuccess(DummyMixer result) {
				System.out.println("getMixer onSuccess");
				sem.release();
			}

			@Override
			public void onError(Throwable cause) {
				System.out.println("getMixer onError: " + cause.getMessage());
			}
		});

		Assert.assertTrue(sem.tryAcquire(500, TimeUnit.MILLISECONDS));
	}

	private static void releaseMediaObject(final MediaObject mo)
			throws IOException, InterruptedException {
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
