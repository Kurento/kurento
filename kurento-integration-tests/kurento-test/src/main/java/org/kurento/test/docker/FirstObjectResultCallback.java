package org.kurento.test.docker;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.core.async.ResultCallbackTemplate;

class FirstObjectResultCallback<E>
		extends ResultCallbackTemplate<FirstObjectResultCallback<E>, E> {

	private static final Logger log = LoggerFactory
			.getLogger(FirstObjectResultCallback.class);

	private E object;
	private CountDownLatch latch = new CountDownLatch(1);

	@Override
	public void onNext(E object) {
		this.object = object;
		latch.countDown();
		try {
			close();
		} catch (IOException e) {
			log.warn("Exception when closing stats cmd stream", e);
		}
	}

	public E waitForObject() throws InterruptedException {
		latch.await();
		return object;
	}
}