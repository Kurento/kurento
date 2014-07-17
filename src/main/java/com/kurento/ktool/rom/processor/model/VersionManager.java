package com.kurento.ktool.rom.processor.model;

public class VersionManager {

	public static String convertToMaven(String version) {
		return version;
	}

	public static String convertToNPM(String version) {
		return version;
	}

	public static boolean isReleaseVersion(String version) {
		return !version.endsWith("-dev");
	}

}
