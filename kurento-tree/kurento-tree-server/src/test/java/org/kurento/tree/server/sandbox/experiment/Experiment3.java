package org.kurento.tree.server.sandbox.experiment;

import org.kurento.tree.server.kms.loadmanager.MaxWebRtcLoadManager;
import org.kurento.tree.server.kmsmanager.FakeFixedNKmsManager;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.sandbox.experiment.framework.Experiment;
import org.kurento.tree.server.sandbox.experiment.framework.TreeManagerCreator;
import org.kurento.tree.server.sandbox.experiment.usage.NSourcesAddRemoveSinksRandomUsage;
import org.kurento.tree.server.treemanager.LessLoadedElasticTM;
import org.kurento.tree.server.treemanager.TreeManager;

public class Experiment3 extends Experiment {

	public void configureExperiment() {

		setKmsManager(new FakeFixedNKmsManager(4, new MaxWebRtcLoadManager(5)));

		addUsageSimulation(new NSourcesAddRemoveSinksRandomUsage(4, 0.8, 0));

		addTreeManagerCreator(new TreeManagerCreator() {
			@Override
			public TreeManager createTreeManager(KmsManager kmsManager) {
				return new LessLoadedElasticTM(kmsManager);
			}
		});
	}

	public static void main(String[] args) {
		new Experiment3().run();
	}
}
