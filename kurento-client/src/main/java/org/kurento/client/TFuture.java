package org.kurento.client;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface TFuture<V> {

	/**
	 * Returns <tt>true</tt> if the transaction associated to this future was
	 * rolled back.
	 *
	 * @return <tt>true</tt> if the transaction associated to this future was
	 *         rolled back.
	 */
	boolean isRollback();

	/**
	 * Returns <tt>true</tt> if the transaction associated to this future is
	 * committed. The transaction can success or fail with exception, in all of
	 * these cases, this method will return <tt>true</tt>.
	 *
	 * @return <tt>true</tt> if the transaction associated to this future is
	 *         committed.
	 */
	boolean isCommitted();

	/**
	 * Waits if necessary for the transaction to be committed, and then
	 * retrieves its result.
	 *
	 * @return the transaction result
	 * @throws CancellationException
	 *             if the transaction was cancelled with rollback
	 * @throws ExecutionException
	 *             if the transaction threw an exception when committed
	 * @throws InterruptedException
	 *             if the current thread was interrupted while waiting
	 */
	V get() throws InterruptedException, ExecutionException;

	/**
	 * Waits if necessary for at most the given time for the transaction to be
	 * committed, and then retrieves its result.
	 *
	 * @param timeout
	 *            the maximum time to wait
	 * @param unit
	 *            the time unit of the timeout argument
	 * @return the computed result
	 * @throws CancellationException
	 *             if the computation was cancelled
	 * @throws ExecutionException
	 *             if the computation threw an exception
	 * @throws InterruptedException
	 *             if the current thread was interrupted while waiting
	 * @throws TimeoutException
	 *             if the wait timed out
	 */
	V get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException;

}
