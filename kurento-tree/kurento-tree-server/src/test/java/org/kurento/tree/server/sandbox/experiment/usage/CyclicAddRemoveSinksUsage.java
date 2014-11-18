package org.kurento.tree.server.sandbox.experiment.usage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.kurento.tree.server.treemanager.TreeManager;

public class CyclicAddRemoveSinksUsage extends UsageSimulation {

	public static class TreeUsage {

		private int iterations;
		private int maxSinksPerTree;
		private TreeManager treeManager;
		private String treeId;

		private boolean created = false;
		private boolean growing = true;
		private int iteration = 0;
		private List<String> sinks = new ArrayList<String>();

		public TreeUsage(int numTree, TreeManager treeManager, int iterations,
				int maxSinksPerTree) {
			this.treeManager = treeManager;
			this.iterations = iterations;
			this.maxSinksPerTree = maxSinksPerTree;
			this.treeId = getTreeId(numTree);
		}

		public void evolve() {

			try {
				if (!created) {
					treeManager.createTree(treeId);
					treeManager.setTreeSource(treeId, "XXX");
					created = true;

				} else if (growing) {

					String sinkId = treeManager.addTreeSink(treeId, "fakeSdp")
							.getId();
					sinks.add(treeId + "|" + sinkId);

					if (sinks.size() == maxSinksPerTree) {
						growing = false;
						System.out.println("Srinking Tree " + treeId);
					}

				} else {
					String sink = sinks.remove(0);
					String[] parts = sink.split("\\|");
					treeManager.removeTreeSink(parts[0], parts[1]);

					if (sinks.isEmpty()) {
						System.out.println("Restarting Tree " + treeId
								+ " in iteration " + iteration);
						treeManager.releaseTree(treeId);
						created = false;
						growing = true;
						iteration++;
					}
				}
			} catch (Exception e) {
				throw e;
			}
		}

		public boolean finished() {
			return iteration >= iterations;
		}
	}

	private int numTrees;
	private int maxSinksPerTree;
	private int iterations;
	private long randomSeed;

	public CyclicAddRemoveSinksUsage(int numTrees, int maxSinksPerTree,
			int iterations, long randomSeed) {
		this.numTrees = numTrees;
		this.maxSinksPerTree = maxSinksPerTree;
		this.iterations = iterations;
		this.randomSeed = randomSeed;
	}

	public CyclicAddRemoveSinksUsage() {
		this(4, 5, 3, 0);
	}

	@Override
	public void useTreeManager(TreeManager treeManager) {

		List<TreeUsage> trees = new ArrayList<>();

		for (int i = 0; i < numTrees; i++) {
			trees.add(new TreeUsage(i, treeManager, this.iterations,
					this.maxSinksPerTree));
		}

		Random r = null;
		if (randomSeed != -1) {
			r = new Random(randomSeed);
		}

		int numTree = -1;
		while (!trees.isEmpty()) {

			TreeUsage tree;
			do {

				if (r != null) {
					numTree = r.nextInt(trees.size());
				} else {
					numTree = (numTree + 1) % trees.size();
				}

				tree = trees.get(numTree);

				if (tree.finished()) {
					trees.remove(tree);
					if (trees.isEmpty()) {
						return;
					} else {
						tree = null;
					}
				}
			} while (tree == null);

			tree.evolve();
		}
	}

	private static String getTreeId(int numTree) {
		return "Tree" + numTree;
	}
}
