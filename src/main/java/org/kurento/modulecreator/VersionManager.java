package org.kurento.modulecreator;

public class VersionManager {

	private static final String DEV_SUFFIX = "-dev";
	private static final int DEV_SUFFIX_LENGTH = DEV_SUFFIX.length();

	public static String convertToMaven(String version) {
		if (isDevelopmentVersion(version)) {
			version = removeDevSuffix(version) + "-SNAPSHOT";
		}
		return version;
	}

	public static String convertToNPM(String version) {
		if (isDevelopmentVersion(version)) {
			version = removeDevSuffix(version);
		}
		return version;
	}

	private static String removeDevSuffix(String version) {
		return version.substring(0, version.length() - DEV_SUFFIX_LENGTH);
	}

	private static boolean isDevelopmentVersion(String version) {
		return !isReleaseVersion(version);
	}

	public static boolean isReleaseVersion(String version) {
		return !version.endsWith(DEV_SUFFIX);
	}

	/**
	 * Compares two version strings.
	 * 
	 * Use this instead of String.compareTo() for a non-lexicographical
	 * comparison that works for version strings. e.g. "1.10".compareTo("1.6").
	 * 
	 * @note It does not work if "1.10" is supposed to be equal to "1.10.0".
	 * 
	 * @param str1
	 *            a string of ordinal numbers separated by decimal points.
	 * @param str2
	 *            a string of ordinal numbers separated by decimal points.
	 * @return The result is a negative integer if str1 is _numerically_ less
	 *         than str2. The result is a positive integer if str1 is
	 *         _numerically_ greater than str2. The result is zero if the
	 *         strings are _numerically_ equal.
	 */
	public static Integer versionCompare(String str1, String str2) {

		String[] vals1 = str1.split("\\.");
		String[] vals2 = str2.split("\\.");

		int i = 0;
		// set index to first non-equal ordinal or length of shortest version
		// string
		while (i < vals1.length && i < vals2.length
				&& vals1[i].equals(vals2[i])) {
			i++;
		}
		// compare first non-equal ordinal number
		if (i < vals1.length && i < vals2.length) {
			int diff = Integer.valueOf(vals1[i]).compareTo(
					Integer.valueOf(vals2[i]));
			return Integer.signum(diff);
		}
		// the strings are equal or one string is a substring of the other
		// e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
		else {
			return Integer.signum(vals1.length - vals2.length);
		}
	}

	public static boolean devCompatibleVersion(String requiredVersion,
			String providedVersion) {

		if (requiredVersion.equals(providedVersion)) {
			return true;
		}

		if (isReleaseVersion(requiredVersion)) {
			return false;
		} else {
			requiredVersion = removeDevSuffix(requiredVersion);
			if (isDevelopmentVersion(providedVersion)) {
				providedVersion = removeDevSuffix(providedVersion);
			}
			return versionCompare(requiredVersion, providedVersion) <= 0
					&& versionCompare(providedVersion,
							incMayorVersion(requiredVersion)) < 0;
		}
	}

	private static String incMayorVersion(String version) {
		String[] nums = version.split("\\.");
		int mayor = Integer.parseInt(nums[0]);

		StringBuilder sb = new StringBuilder(Integer.toString(mayor + 1));
		for (int i = 1; i < nums.length; i++) {
			sb.append(".0");
		}
		return sb.toString();
	}
}
