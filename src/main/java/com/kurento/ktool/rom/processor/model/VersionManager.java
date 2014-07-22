package com.kurento.ktool.rom.processor.model;

public class VersionManager {

	private static final String DEV_SUFFIX = "-dev";
	private static final int DEV_SUFFIX_LENGTH = DEV_SUFFIX.length();

	public static String convertToMaven(String version) {
		if (!isReleaseVersion(version)) {
			version = version
					.substring(0, version.length() - DEV_SUFFIX_LENGTH)
					+ "-SNAPSHOT";
		}
		return version;
	}

	public static String convertToNPM(String version) {
		if (!isReleaseVersion(version)) {
			version = version
					.substring(0, version.length() - DEV_SUFFIX_LENGTH);
		}
		return version;
	}

	public static boolean isReleaseVersion(String version) {
		return !version.endsWith(DEV_SUFFIX);
	}

}
