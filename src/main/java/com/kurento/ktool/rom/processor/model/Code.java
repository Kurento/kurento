package com.kurento.ktool.rom.processor.model;

import java.util.Map;

import com.kurento.ktool.rom.processor.codegen.ModelManager;

public class Code {

	private Map<String, Map<String, String>> kmd;
	private Map<String, Map<String, String>> api;
	private Map<String, Map<String, String>> implementation;

	public void completeInfo(ModelManager modelManager) {
		System.out.println("Code: " + this);
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

	public Map<String, Map<String, String>> getImplementation() {
		return implementation;
	}
}
