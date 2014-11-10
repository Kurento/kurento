package org.kurento.tree.server.treemanager;

public abstract class AbstractOneTreeManager implements TreeManager {

	private static final String DEFAULT_TREE_ID = "TreeId";

	private String treeId;

	protected boolean createdTree = false;

	public AbstractOneTreeManager() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public synchronized String createTree() throws TreeException {
		if (createdTree) {
			throw new TreeException(
					"AotOneTreeManager "
							+ " can only create one tree and this tree was previously created");
		}
		treeId = DEFAULT_TREE_ID;
		createdTree = true;
		return treeId;
	}

	@Override
	public void createTree(String treeId) throws TreeException {
		if (createdTree) {
			throw new TreeException(
					"AotOneTreeManager "
							+ " can only create one tree and this tree was previously created");
		}
		this.treeId = treeId;
		createdTree = true;
	}

	protected void checkTreeId(String treeId) throws TreeException {
		if (!this.treeId.equals(treeId)) {
			throw new TreeException("Unknown tree '" + treeId + "'");
		}
	}
}
