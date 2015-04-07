package org.kurento.jsonrpc.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.kurento.jsonrpc.internal.server.PingWatchdogManager;
import org.kurento.jsonrpc.internal.server.PingWatchdogManager.NativeSessionCloser;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class PingWatchdogManagerTest {

	@Test
	public void test() throws InterruptedException {

		ThreadPoolTaskScheduler executor = new ThreadPoolTaskScheduler();
		executor.initialize();

		NativeSessionCloser closer = mock(NativeSessionCloser.class);
		PingWatchdogManager manager = new PingWatchdogManager(executor, closer);

		manager.setPingWatchdog(true);
		
		for (int i = 0; i < 10; i++) {
			manager.pingReceived("TransportID");
			Thread.sleep(100);
		}

		Thread.sleep(500);

		verify(closer).closeSession("TransportID");
	}

}
