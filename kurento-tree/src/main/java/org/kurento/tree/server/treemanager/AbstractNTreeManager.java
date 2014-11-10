package org.kurento.tree.server.treemanager;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.tree.client.TreeEndpoint;

public abstract class AbstractNTreeManager implements TreeManager {

	public abstract class TreeInfo {

		public abstract void release();

		public abstract String setTreeSource(String offerSdp);

		public abstract void removeTreeSource();

		public abstract TreeEndpoint addTreeSink(String sdpOffer);

		public abstract void removeTreeSink(String sinkId);
	}

	private final TreeInfo DUMMY_TREE_INFO = new TreeInfo() {
		@Override
		public void release() {
		}

		@Override
		public String setTreeSource(String offerSdp) {
			return null;
		}

		@Override
		public void removeTreeSource() {
		}

		@Override
		public TreeEndpoint addTreeSink(String sdpOffer) {
			return null;
		}

		@Override
		public void removeTreeSink(String sinkId) {
		}
	};

	private ConcurrentHashMap<String, TreeInfo> trees = new ConcurrentHashMap<>();

	public AbstractNTreeManager() {
	}

	@Override
	public String createTree() throws TreeException {

		String treeId = UUID.randomUUID().toString();
		trees.put(treeId, createTreeInfo(treeId));
		return treeId;
	}

	protected abstract TreeInfo createTreeInfo(String treeId);

	@Override
	public void createTree(String treeId) throws TreeException {

		TreeInfo prevTreeInfo = trees.putIfAbsent(treeId, DUMMY_TREE_INFO);
		if (prevTreeInfo != null) {
			throw new TreeException("Tree with id '" + treeId
					+ "' already exists. Try another one");
		} else {
			trees.replace(treeId, createTreeInfo(treeId));
		}
	}

	@Override
	public synchronized void releaseTree(String treeId) throws TreeException {
		getTreeInfo(treeId).release();
	}

	@Override
	public synchronized String setTreeSource(String treeId, String offerSdp)
			throws TreeException {
		return getTreeInfo(treeId).setTreeSource(offerSdp);
	}

	@Override
	public synchronized void removeTreeSource(String treeId)
			throws TreeException {
		getTreeInfo(treeId).removeTreeSource();
	}

	@Override
	public synchronized TreeEndpoint addTreeSink(String treeId, String sdpOffer)
			throws TreeException {
		return getTreeInfo(treeId).addTreeSink(sdpOffer);
	}

	@Override
	public synchronized void removeTreeSink(String treeId, String sinkId)
			throws TreeException {

		getTreeInfo(treeId).removeTreeSink(sinkId);
	}

	protected TreeInfo getTreeInfo(String treeId) {
		TreeInfo treeInfo = trees.get(treeId);
		if (treeInfo == null) {
			throw new TreeException("Tree with id '" + treeId + "' not found");
		} else {
			return treeInfo;
		}
	}
}
