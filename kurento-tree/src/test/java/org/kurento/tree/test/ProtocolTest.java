package org.kurento.tree.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.kurento.jsonrpc.client.JsonRpcClientLocal;
import org.kurento.tree.client.KurentoTreeClient;
import org.kurento.tree.protocol.TreeEndpoint;
import org.kurento.tree.server.app.ClientsJsonRpcHandler;
import org.kurento.tree.server.treemanager.TreeException;
import org.kurento.tree.server.treemanager.TreeManager;

public class ProtocolTest {

	private TreeManager treeMgr;
	private KurentoTreeClient client;

	@Before
	public void init() {

		treeMgr = mock(TreeManager.class);
		client = new KurentoTreeClient(new JsonRpcClientLocal(
				new ClientsJsonRpcHandler(treeMgr)));
	}

	@Test
	public void testCreateTree() throws IOException, TreeException {

		when(treeMgr.createTree()).thenReturn("TreeId");
		assertThat(client.createTree(), is("TreeId"));
	}

	@Test
	public void testSetTreeSource() throws IOException, TreeException {

		when(treeMgr.setTreeSource("TreeId", "sdpOffer")).thenReturn(
				"sdpAnswer");

		assertThat(client.setTreeSource("TreeId", "sdpOffer"), is("sdpAnswer"));
	}

	@Test
	public void testAddTreeSink() throws IOException, TreeException {

		when(treeMgr.addTreeSink("TreeId", "sdpOffer")).thenReturn(
				new TreeEndpoint("SinkId", "sdpAnswer"));

		assertThat(client.addTreeSink("TreeId", "sdpOffer"),
				is(new TreeEndpoint("SinkId", "sdpAnswer")));
	}

	@Test
	public void testReleaseTree() throws IOException, TreeException {

		client.releaseTree("TreeId");
		verify(treeMgr).releaseTree("TreeId");
	}

	@Test
	public void testRemoveSink() throws IOException, TreeException {

		client.removeTreeSink("TreeId", "SinkId");
		verify(treeMgr).removeTreeSink("TreeId", "SinkId");
	}

	@Test
	public void testRemoveSource() throws IOException, TreeException {

		client.removeTreeSource("TreeId");
		verify(treeMgr).removeTreeSource("TreeId");
	}
}
