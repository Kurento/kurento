package org.kurento.tree.server.sandbox.experiment;

import org.kurento.tree.server.kms.loadmanager.MaxWebRtcLoadManager;
import org.kurento.tree.server.kmsmanager.FakeElasticKmsManager;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.sandbox.experiment.framework.Experiment;
import org.kurento.tree.server.sandbox.experiment.framework.TreeManagerCreator;
import org.kurento.tree.server.sandbox.experiment.usage.NSourcesAddRemoveSinksRandomUsage;
import org.kurento.tree.server.treemanager.LessLoadedElasticTM;
import org.kurento.tree.server.treemanager.TreeManager;

public class Experiment4 extends Experiment {

	public void configureExperiment() {

		setKmsManager(new FakeElasticKmsManager(0.9, 2, 5,
				new MaxWebRtcLoadManager(4)));

		addUsageSimulation(new NSourcesAddRemoveSinksRandomUsage(4, 0.8, 0));

		addTreeManagerCreator(new TreeManagerCreator() {
			@Override
			public TreeManager createTreeManager(KmsManager kmsManager) {
				return new LessLoadedElasticTM(kmsManager);
			}
		});
	}

	public static void main(String[] args) {
		new Experiment4().run();
	}
}
