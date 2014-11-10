package org.kurento.tree.server.treemanager;

import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.client.TreeException;
import org.kurento.tree.server.kmsmanager.KmsManager;

public interface TreeManager {

	public String createTree() throws TreeException;

	public void createTree(String treeId) throws TreeException;

	public void releaseTree(String treeId) throws TreeException;

	public String setTreeSource(String treeId, String sdpOffer)
			throws TreeException;

	public void removeTreeSource(String treeId) throws TreeException;

	public TreeEndpoint addTreeSink(String treeId, String sdpOffer)
			throws TreeException;

	public void removeTreeSink(String treeId, String sinkId)
			throws TreeException;

	public KmsManager getKmsManager();

}
