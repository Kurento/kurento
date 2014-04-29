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
package com.kurento.test.player;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.client.ClientProtocolException;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.kurento.demo.internal.VideoURLs;
import com.kurento.test.base.BaseArquillianTst;

/**
 * Integration test (JUnit/Arquillian) for concurrent HTTP Player. It uses
 * several <code>HttpClient</code> (depending on the value of
 * <code>nThreads</code> field to interact with Kurento Application Server
 * (KAS).
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.0
 * @see <a href="http://hc.apache.org/">Apache HTTP Components</a>
 */

@RunWith(Arquillian.class)
public class ParallelPlayerIT extends BaseArquillianTst {

	private static final int nThreads = 5;

	@Test
	public void testParallelPlayRedirect() throws ClientProtocolException,
			IOException, InterruptedException, ExecutionException {
		for (String video : VideoURLs.small) {
			testParallelPlay("player-redirect/" + video, 200, "video/webm",
					false, null);
		}
	}

	@Test
	public void testParallelPlayTunnel() throws ClientProtocolException,
			IOException, InterruptedException, ExecutionException {
		for (String video : VideoURLs.small) {
			testParallelPlay("player-tunnel/" + video, 200, "video/webm",
					false, null);
		}
	}

	@Test
	public void testParallelRejectRedirect() throws ClientProtocolException,
			IOException, InterruptedException, ExecutionException {
		testParallelPlay("player-redirect/reject", 407, null, false, null);
	}

	@Test
	public void testParallelRejectTunnel() throws ClientProtocolException,
			IOException, InterruptedException, ExecutionException {
		testParallelPlay("player-tunnel/reject", 407, null, false, null);
	}

	@Test
	public void testParallelInterruptRedirect() throws ClientProtocolException,
			IOException, InterruptedException, ExecutionException {
		testParallelPlay("player-redirect", 200, null, true, null);
	}

	@Test
	public void testParallelInterruptTunnel() throws ClientProtocolException,
			IOException, InterruptedException, ExecutionException {
		testParallelPlay("player-tunnel", 200, null, true, null);
	}

	private void testParallelPlay(String url, int statusCode,
			String contentType, boolean interrupt, String[] expectedHandlerFlow)
			throws ClientProtocolException, IOException, InterruptedException,
			ExecutionException {
		ExecutorService execute = Executors.newFixedThreadPool(nThreads);
		Collection<Future<?>> futures = new LinkedList<Future<?>>();

		// Perform nThreads calls
		for (int i = 0; i < nThreads; i++) {
			futures.add(execute.submit(new PlayerTst(url, getServerPort(),
					statusCode, contentType, interrupt, expectedHandlerFlow)));
		}

		// Wait for all threads to be terminated
		for (Future<?> future : futures) {
			future.get();
		}
	}

}
