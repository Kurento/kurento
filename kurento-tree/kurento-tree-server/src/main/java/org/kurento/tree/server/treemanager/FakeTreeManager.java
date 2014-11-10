package org.kurento.tree.server.treemanager;

import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.client.TreeException;
import org.kurento.tree.server.kmsmanager.KmsManager;

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

	@Override
	public KmsManager getKmsManager() {
		return null;
	}

	@Override
	public void createTree(String treeId) throws TreeException {
	}
}
