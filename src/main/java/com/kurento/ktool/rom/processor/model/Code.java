package com.kurento.ktool.rom.processor.model;

import java.util.HashMap;
import java.util.Map;

import com.kurento.ktool.rom.processor.codegen.ModelManager;

public class Code {

	private Map<String, Map<String, String>> kmd;
	private Map<String, Map<String, String>> api;
	private Map<String, String> implementation;

	public void completeInfo(Model model, ModelManager modelManager) {

		if (api == null) {
			api = new HashMap<>();
		}

		if (implementation == null) {
			implementation = new HashMap<>();
		}

		putDefault(api, "java", "packageName",
				"org.kurento.plugin." + model.getName());
		putDefault(api, "java", "maven.groupId", "org.kurento.plugin");
		putDefault(api, "java", "maven.artifactId", model.getName());
		putDefault(api, "java", "maven.version",
				VersionManager.convertToMaven(model.getVersion()));

		putDefault(api, "js", "node.name", "kws-plugin-" + model.getName());
		putDefault(api, "js", "npm.version",
				VersionManager.convertToNPM(model.getVersion()));
		putDefault(api, "js", "npm.description", "");

		putDefault(implementation, "cpp.namespace",
				"kurento::plugin::" + model.getName());
		putDefault(implementation, "lib", "libkms" + model.getName());
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
