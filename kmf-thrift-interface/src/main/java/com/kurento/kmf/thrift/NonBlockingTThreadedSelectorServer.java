package com.kurento.kmf.thrift;

import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;

/**
 * Decorator of the {@link TThreadedSelectorServer}. This server does not block
 * when invoking the {@link TServer#serve} method. The {@code ExecutorService}
 * will not be closed by the server, and it is a task of the developer to
 * gracefully shut down the service passed to the server
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 */
public class NonBlockingTThreadedSelectorServer extends TThreadedSelectorServer {

	/**
	 * @param args
	 */
	public NonBlockingTThreadedSelectorServer(Args args) {
		super(args);
	}

	/**
	 * Override of the {@link TThreadedSelectorServer#serve} method. This is a
	 * non-blocking call, achieved by skipping the call to
	 * {@code waitForShutdown()}.
	 */
	@Override
	public void serve() {
		// start any IO threads
		if (!startThreads()) {
			throw new KurentoMediaFrameworkException(
					"Could not start thread in Thrift server", 30001);
		}
		// start listening, or exit
		if (!startListening()) {
			throw new KurentoMediaFrameworkException(
					"Could not start listening in Thrift server", 30002);
		}
		setServing(true);
	}

	@Override
	public void stop() {
		setServing(false);
		super.stop();
	}
}