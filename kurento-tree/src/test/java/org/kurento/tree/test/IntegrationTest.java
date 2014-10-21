package org.kurento.tree.test;

import org.junit.Ignore;
import org.junit.Test;
import org.kurento.tree.client.KurentoTreeClient;

public class IntegrationTest {

	@Test
	@Ignore
	public void test() throws Exception {

//		System.setProperty(KurentoTreeServerApp.KMSS_URIS_PROPERTY,
//				"[\"ws://localhost:8888/kurento\","
//						+ "\"ws://localhost:8888/kurento\"]");

		final KurentoTreeClient client = new KurentoTreeClient(
				"ws://localhost:8890/kurento-tree");

		final String treeId = client.createTree();

		final String sdpOffer = "v=0\r\n"
		          + "o=- 12345 12345 IN IP4 95.125.31.136\r\n"
		          + "s=-\r\n"
		          + "c=IN IP4 95.125.31.136\r\n"
		          + "t=0 0\r\n"
		          + "m=video 52126 RTP/AVP 96 97 98\r\n"
		          + "a=rtpmap:96 H264/90000\r\n"
		          + "a=rtpmap:97 MP4V-ES/90000\r\n"
		          + "a=rtpmap:98 H263-1998/90000\r\n"
		          + "a=recvonly\r\n"
		          + "b=AS:384\r\n";

		client.setTreeSource(treeId, sdpOffer);
		
		for (int i = 0; i < 3; i++) {
			client.addTreeSink(treeId, sdpOffer).getSdp();
		}

		System.out.println("Browsers created");

	}

}
