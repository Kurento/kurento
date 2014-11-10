package org.kurento.tree.server.sandbox;

import org.kurento.tree.server.app.KurentoTreeServerApp;

public class NoKmssTreeServerApp {

	public static void main(String[] args) throws Exception {

		System.setProperty(KurentoTreeServerApp.KMSS_URIS_PROPERTY, "[]");

		KurentoTreeServerApp.start();
	}
}
