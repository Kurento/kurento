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
import java.security.NoSuchAlgorithmException;

import org.apache.http.client.ClientProtocolException;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.kurento.test.base.BaseArquillianTst;

/**
 * Integration test (JUnit/Arquillian) for HTTP Player. It uses
 * <code>HttpClient</code> to interact with Kurento Application Server (KAS).
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.0
 * @see <a href="http://hc.apache.org//">Apache HTTP Component</a>
 */
@RunWith(Arquillian.class)
public class PlayerIT extends BaseArquillianTst {

	@Test
	public void testPlayRedirect() throws ClientProtocolException, IOException,
			NoSuchAlgorithmException {
		testPlay("player-redirect/small-webm", 200, "video/webm", false, null);
	}

	@Test
	public void testPlayTunnel() throws ClientProtocolException, IOException,
			NoSuchAlgorithmException {
		testPlay("player-tunnel/small-mp4", 200, "video/webm", false, null);
	}

	@Test
	public void testRejectRedirect() throws ClientProtocolException,
			IOException, NoSuchAlgorithmException {
		testPlay("player-redirect/reject", 407, null, false, null);
	}

	@Test
	public void testRejectTunnel() throws ClientProtocolException, IOException,
			NoSuchAlgorithmException {
		testPlay("player-tunnel/reject", 407, null, false, null);
	}

	@Test
	public void testInterruptRedirect() throws ClientProtocolException,
			IOException, NoSuchAlgorithmException {
		testPlay("player-redirect", 200, null, true, null);
	}

	@Test
	public void testInterruptTunnel() throws ClientProtocolException,
			IOException, NoSuchAlgorithmException {
		testPlay("player-tunnel", 200, null, true, null);
	}

	private void testPlay(String url, int statusCode, String contentType,
			boolean interrupt, String[] expectedHandlerFlow)
			throws ClientProtocolException, IOException,
			NoSuchAlgorithmException {
		PlayerTst playerTst = new PlayerTst(url, getServerPort(), statusCode,
				contentType, interrupt, expectedHandlerFlow);
		playerTst.run();
	}
}
