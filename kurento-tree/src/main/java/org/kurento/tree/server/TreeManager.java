package org.kurento.tree.server;

import org.kurento.tree.protocol.TreeEndpoint;

public interface TreeManager {

	public String createTree() throws TreeException;

	public void releaseTree(String treeId) throws TreeException;

	public String setTreeSource(String treeId, String sdpOffer)
			throws TreeException;

	public void removeTreeSource(String treeId) throws TreeException;

	public TreeEndpoint addTreeSink(String treeId, String sdpOffer)
			throws TreeException;

	public void removeTreeSink(String treeId, String sinkId)
			throws TreeException;

}
