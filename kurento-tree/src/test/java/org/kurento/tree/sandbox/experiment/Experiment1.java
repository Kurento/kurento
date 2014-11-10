package org.kurento.tree.sandbox.experiment;

import org.kurento.tree.sandbox.OneSourceAddRemoveSinks;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.treemanager.LessLoadedOneElasticTreeManager;
import org.kurento.tree.server.treemanager.TreeManager;

public class Experiment1 extends Experiment {

	public void configureExperiment() {

		// setKmsManager(new FakeFixedKmsManager(4));
		setKmsManager(new FakeElasticKmsManager(0.8));

		addUsageSimulation(new OneSourceAddRemoveSinks());

		addTreeManagerCreator(new TreeManagerCreator() {
			@Override
			public TreeManager createTreeManager(KmsManager kmsManager) {
				return new LessLoadedOneElasticTreeManager(kmsManager);
			}
		});
	}

	public static void main(String[] args) {
		new Experiment1().run();
	}
}
