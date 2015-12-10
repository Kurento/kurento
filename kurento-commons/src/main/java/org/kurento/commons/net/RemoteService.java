package org.kurento.commons.net;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RemoteService {

	public static void waitForReady(String host, int port, int time,
			TimeUnit unit)
					throws UnknownHostException, IOException, TimeoutException {

		long maxTime = System.currentTimeMillis() + unit.toMillis(time);

		while (true) {
			try {
				Socket client = new Socket(host, port);
				client.close();
				break;
			} catch (ConnectException ce) {

				if (System.currentTimeMillis() > maxTime) {
					throw new TimeoutException();
				}

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
		}
	}

}
