package org.kurento.modulecreator.test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.junit.Test;
import org.kurento.modulecreator.KurentoModuleCreator;
import org.kurento.modulecreator.KurentoModuleCreatorException;
import org.kurento.modulecreator.PathUtils;
import org.kurento.modulecreator.Result;

public class CodeGenerationTest {

	static String[] TEMPLATES = { "cpp_cmake_dependencies", "cpp_find_cmake",
			"cpp_interface", "cpp_interface_internal", "cpp_module",
			"cpp_pkgconfig", "cpp_server", "cpp_server_internal", "doc",
			"maven", "npm" };

	@Test
	public void test() throws IOException, URISyntaxException {

		KurentoModuleCreator modCreator = new KurentoModuleCreator();

		modCreator.addKmdFileToGen(PathUtils
				.getPathInClasspath("/core.kmd.json"));

		for (String template : TEMPLATES) {
			System.out.println("Template:" + template);
			modCreator.setInternalTemplates(template);
			modCreator.setCodeGenDir(Paths.get("/tmp/test_java"));
			Result r = modCreator.generateCode();
			if (!r.isSuccess()) {
				r.showErrorsInConsole();
				throw new KurentoModuleCreatorException(r.toString());
			}
		}
	}
}
