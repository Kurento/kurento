package org.kurento.tree.server;

import org.kurento.tree.protocol.TreeEndpoint;

public class FakeTreeManager implements TreeManager {

	@Override
	public String createTree() {
		return "1";
	}

	@Override
	public void releaseTree(String treeId) throws TreeException {
	}

	@Override
	public String setTreeSource(String treeId, String offerSdp)
			throws TreeException {
		return "sdp";
	}

	@Override
	public void removeTreeSource(String treeId) throws TreeException {
	}

	@Override
	public TreeEndpoint addTreeSink(String treeId, String offerSdp)
			throws TreeException {
		return new TreeEndpoint("sdp", "id");
	}

	@Override
	public void removeTreeSink(String treeId, String sinkId)
			throws TreeException {
	}
}
