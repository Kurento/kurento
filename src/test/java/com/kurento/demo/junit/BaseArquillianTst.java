package com.kurento.demo.junit;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class BaseArquillianTst {

	public static final Log log = LogFactory.getLog(BaseArquillianTst.class);

	@Deployment
	public static WebArchive createDeployment() {
		WebArchive war = ShrinkWrap
				.create(ZipImporter.class, "content-demo.war")
				.importFrom(new File("target/content-demo-1.0.0-SNAPSHOT.war"))
				.as(WebArchive.class).addPackages(true, "com.kurento");
		log.info(war.toString(true));
		return war;
	}

}
