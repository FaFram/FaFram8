package org.jboss.fuse.qa.fafram8.util;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Util class for masking passwords in string.
 * Created by avano on 10.3.17.
 */
@Slf4j
public final class PasswordUtils {
	private static final String[] PASSWORD_OPTIONS = new String[] {"--zookeeper-password", "password", "--password",
			"jmx-password", "--pass-phrase", "--jmx-password"};
	/**
	 * Constructor.
	 */
	private PasswordUtils() {
	}

	/**
	 * Masks the password in string.
	 * @param input input string
	 * @return string with masked password
	 */
	public static String maskPassword(String input) {
		if (input == null || input.isEmpty()) {
			return input;
		}

		for (String opt : PASSWORD_OPTIONS) {
			if (input.contains(" " + opt)) {
				String password = StringUtils.substringBetween(input, " " + opt + " ", " ");
				if (password == null || password.isEmpty()) {
					// Probably when this option is the last one
					password = StringUtils.substringAfter(input, " " + opt + " ");
					if (password == null || password.isEmpty()) {
						log.error("Couldn't parse password for option \'" + opt + "\', not masking anything!");
						return input;
					}
				}
				if (password.startsWith("\"") && password.endsWith("\"")) {
					input = input.replaceAll(" " + opt + " " + password, " " + opt + " \"" + getAsterisks(password.length() - 2) + "\"");
				} else {
					input = input.replaceAll(" " + opt + " " + password, " " + opt + " " + getAsterisks(password.length()));
				}
			}
		}

		return input;
	}

	/**
	 * Masks password in map's toString output.
	 * @param mapToString toString output
	 * @return output with masked passwords
	 */
	public static String maskMap(String mapToString) {
		if (mapToString == null || mapToString.isEmpty()) {
			return mapToString;
		}

		for (String opt : PASSWORD_OPTIONS) {
			if (mapToString.contains(" " + opt) || mapToString.contains("{" + opt)) {
				String password;
				if ("password".equals(opt)) {
					password = StringUtils.substringBetween(mapToString, " " + opt + "=[", "]");
					if (password == null) {
						password = StringUtils.substringBetween(mapToString, "{" + opt + "=[", "]");
					}
				} else {
					password = StringUtils.substringBetween(mapToString, opt + "=[", "]");
				}
				if ("password".equals(opt)) {
					mapToString = mapToString.replaceAll("\\{" + opt + "=\\[" + password, "{" + opt + "=[" + getAsterisks(password.length()));
					mapToString = mapToString.replaceAll(" " + opt + "=\\[" + password, " " + opt + "=[" + getAsterisks(password.length()));
				} else {
					mapToString = mapToString.replaceAll(opt + "=\\[" + password, opt + "=[" + getAsterisks(password.length()));
				}
			}
		}

		return mapToString;
	}

	/**
	 * Returns asterisks according to string length.
	 * @param s string
	 * @return masked string
	 */
	public static String maskString(String s) {
		return getAsterisks(s.length());
	}

	/**
	 * Returns string with given amount of asterisks.
	 * @param count asterisks count
	 * @return string with given amount of asterisks
	 */
	private static String getAsterisks(int count) {
		if (count == 0) {
			return "";
		}
		return "*" + getAsterisks(count - 1);
	}
}
