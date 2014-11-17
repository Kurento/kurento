package org.kurento.tree.server.sandbox.experiment;

import org.kurento.tree.server.kms.loadmanager.MaxWebRtcLoadManager;
import org.kurento.tree.server.kmsmanager.FakeFixedNKmsManager;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.sandbox.experiment.framework.Experiment;
import org.kurento.tree.server.sandbox.experiment.framework.TreeManagerCreator;
import org.kurento.tree.server.sandbox.experiment.usage.NSourcesAddRemoveSinksRandomUsage;
import org.kurento.tree.server.treemanager.OneKmsTM;
import org.kurento.tree.server.treemanager.TreeManager;

public class Experiment0 extends Experiment {

	public void configureExperiment() {

		setKmsManager(new FakeFixedNKmsManager(1, new MaxWebRtcLoadManager(15)));

		addUsageSimulation(new NSourcesAddRemoveSinksRandomUsage());

		addTreeManagerCreator(new TreeManagerCreator() {
			@Override
			public TreeManager createTreeManager(KmsManager kmsManager) {
				return new OneKmsTM(kmsManager);
			}
		});
	}

	public static void main(String[] args) {
		new Experiment0().run();
	}
}
