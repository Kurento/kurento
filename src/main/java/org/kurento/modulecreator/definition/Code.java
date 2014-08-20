package org.kurento.modulecreator.definition;

import java.util.HashMap;
import java.util.Map;

import org.kurento.modulecreator.ModuleManager;
import org.kurento.modulecreator.VersionManager;

public class Code {

	private Map<String, Map<String, String>> kmd;
	private Map<String, Map<String, String>> api;
	private Map<String, String> implementation;

	public void completeInfo(ModuleDefinition module,
			ModuleManager moduleManager) {

		if (api == null) {
			api = new HashMap<>();
		}

		putDefault(api, "java", "packageName",
				"org.kurento.module." + module.getName());
		putDefault(api, "java", "mavenGroupId", "org.kurento.module");
		putDefault(api, "java", "mavenArtifactId", module.getName());
		putDefault(api, "java", "mavenVersion",
				VersionManager.convertToMaven(module.getVersion()));

		putDefault(api, "js", "nodeName", "kurento-module-" + module.getName());

		String npmGit = module.getCode().getApi().get("js").get("npmGit");

		putDefault(api, "js", "npmVersion",
				VersionManager.convertToNpm(npmGit, module.getVersion()));

		putDefault(api, "js", "npmDescription", "");

		if (implementation == null) {
			implementation = new HashMap<>();
		}

		putDefault(implementation, "cppNamespace", "kurento::module::"
				+ module.getName());
		putDefault(implementation, "lib", "libkms"
				+ module.getName().toLowerCase());

		if (kmd != null) {
			putDefault(kmd, "java", "mavenGroupId", "org.kurento.module");
			putDefault(kmd, "java", "mavenArtifactId", module.getName()
					+ ".kmd");
			putDefault(kmd, "java", "mavenVersion",
					VersionManager.convertToMaven(module.getVersion()));
		}
	}

	private void putDefault(Map<String, String> section, String key,
			String defaultValue) {
		if (section.get(key) == null) {
			section.put(key, defaultValue);
		}
	}

	private void putDefault(Map<String, Map<String, String>> section,
			String subsection, String key, String defaultValue) {

		Map<String, String> subsectionMap = section.get(subsection);
		if (subsectionMap == null) {
			subsectionMap = new HashMap<>();
			section.put(subsection, subsectionMap);
			subsectionMap.put(key, defaultValue);
		} else {
			if (subsectionMap.get(key) == null) {
				subsectionMap.put(key, defaultValue);
			}
		}
	}

	@Override
	public String toString() {
		return "Code [kmd=" + kmd + ", api=" + api + ", implementation="
				+ implementation + "]";
	}

	public Map<String, Map<String, String>> getKmd() {
		return kmd;
	}

	public Map<String, Map<String, String>> getApi() {
		return api;
	}

	public Map<String, String> getImplementation() {
		return implementation;
	}
}
