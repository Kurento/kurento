/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
package org.kurento.test.internal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Abortable count down latch. If the latch is aborted, all threads will be
 * signaled to continue and the await method will raise a
 * {@link AbortedException}
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Ivan Gracia (igracia@kurento.org)
 * @since 6.0.0
 */
public class AbortableCountDownLatch extends CountDownLatch {
	private boolean aborted;

	private long remainingLatchesCount;

	public AbortableCountDownLatch(int count) {
		super(count);
	}

	/**
	 * Unblocks all threads waiting on this latch and cause them to receive an
	 * AbortedException. If the latch has already counted all the way down, this
	 * method does nothing.
	 */
	public void abort() {
		if (getCount() == 0) {
			return;
		}

		this.aborted = true;
		this.remainingLatchesCount = getCount();
		while (getCount() > 0) {
			countDown();
		}
	}

	@Override
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
		final boolean rtrn = super.await(timeout, unit);
		if (aborted) {
			throw new AbortedException();
		}
		return rtrn;
	}

	@Override
	public void await() throws InterruptedException {
		super.await();
		if (aborted) {
			throw new AbortedException();
		}
	}

	public long getRemainingLatchesCount() {
		return remainingLatchesCount;
	}

	public static class AbortedException extends InterruptedException {

		private static final long serialVersionUID = 5426681873843162292L;

		public AbortedException() {
		}

		public AbortedException(String detailMessage) {
			super(detailMessage);
		}
	}
}
