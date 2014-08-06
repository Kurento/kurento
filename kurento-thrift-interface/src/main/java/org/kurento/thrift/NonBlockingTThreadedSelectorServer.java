package org.kurento.thrift;

import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;

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
			throw new ThriftServerException(
					"Error starting non blocking selector server: Could not start threads");
		}
		// start listening, or exit
		if (!startListening()) {
			throw new ThriftServerException(
					"Error starting non blocking selector server: Could not start listening");
		}
		setServing(true);
	}

	@Override
	public void stop() {
		setServing(false);
		super.stop();
	}
}