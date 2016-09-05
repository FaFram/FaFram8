package org.jboss.fuse.qa.fafram8.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Util class for working with container options map.
 * Created by avano on 22.6.16.
 */
@Slf4j
public final class OptionUtils {
	/**
	 * Private constructor.
	 */
	private OptionUtils() {
	}

	/**
	 * Persists the value in the specified map. If the value already exists it checks the singleValued attribute
	 * and if it is true, then it overwrites the entry, otherwise it adds it to the list.
	 * @param map options map
	 * @param option option
	 * @param values values to insert
	 */
	public static void set(Map<Option, List<String>> map, Option option, String... values) {
		if (map.containsKey(option) && !option.isSingleValued()) {
			map.get(option).addAll(trim(option, values));
		} else {
			map.put(option, trim(option, values));
		}
	}

	/**
	 * Persists the value in the specified map. If the value already exists it checks the singleValued attribute
	 * and if it is true, then it overwrites the entry, otherwise it adds it to the list.
	 * @param map options map
	 * @param option option
	 * @param values values to insert
	 */
	public static void set(Map<Option, List<String>> map, Option option, List<String> values) {
		set(map, option, values.toArray(new String[values.size()]));
	}

	/**
	 * Removes the switch if presents and trims the entry.
	 * @param option option
	 * @param values values array
	 * @return list of values
	 */
	private static List<String> trim(Option option, String... values) {
		final List<String> ret = new ArrayList<>();
		for (String value : values) {
			// For example if it contains \"--env \", this will be trimed out as the switch will be automatically added
			if (value.contains(option.toString() + " ")) {
				ret.add(value.replace(option.toString(), "").trim());
			} else {
				ret.add(value);
			}
		}

		return ret;
	}

	/**
	 * Gets the create command from the map gathering the switches.
	 * @param map options map
	 * @return container create command
	 */
	public static String getCommand(Map<Option, List<String>> map) {
		final StringBuilder builder = new StringBuilder();
		for (Map.Entry<Option, List<String>> entry : map.entrySet()) {
			if (!entry.getKey().toString().isEmpty() && !entry.getKey().toString().startsWith("--")) {
				// Do not include the ones without --
				continue;
			}
			if (entry.getValue().size() == 0) {
				// If the are no arguments in the list, skip
				continue;
			}
			if (Option.OTHER.equals(entry.getKey())) {
				// Do not put the value of the enum here, as the OTHER is just for backwards compatibility and it just prints
				// everything in it
				for (String s : entry.getValue()) {
					builder.append(s).append(" ");
				}
				continue;
			}

			switch (entry.getKey()) {
				case PROFILE:
				case ENV:
				case DATASTORE_OPTION:
				case FALLBACK_REPOS:
					// You need to include the switch over and over again for each value
					for (String s : entry.getValue()) {
						builder.append(entry.getKey()).append(" \"").append(s).append("\" ");
					}
					break;
				default:
					// Special usecase for just flag without values
					if (entry.getValue().size() == 1 && entry.getValue().get(0).isEmpty()) {
						builder.append(entry.getKey()).append(" ");
						continue;
					}
					// One switch for all values
					builder.append(entry.getKey()).append(" \"");
					for (String s : entry.getValue()) {
						builder.append(s).append(" ");
					}
					builder.deleteCharAt(builder.length() - 1).append("\" ");
					break;
			}
		}
		return builder.toString();
	}

	/**
	 * Gets the list based on the option. If the list does not exist yet, it will be created, inserted into the map and returned.
	 * @param map options map
	 * @param option option
	 * @return entries list for specified option
	 */
	public static List<String> get(Map<Option, List<String>> map, Option option) {
		if (map.get(option) == null) {
			final List<String> newList = new ArrayList<>();
			map.put(option, newList);
			return newList;
		}
		return map.get(option);
	}

	/**
	 * Does the same as get(Map, Option), but returns first string from the list.
	 * @param map options map
	 * @param option option
	 * @return first string from the list
	 */
	public static String getString(Map<Option, List<String>> map, Option option) {
		final List<String> list = get(map, option);
		if (list.isEmpty()) {
			return "";
		} else {
			return list.get(0);
		}
	}

	/**
	 * Overwrite the specified option with new values.
	 * @param map options map
	 * @param option option
	 * @param newValue new value
	 */
	public static void overwrite(Map<Option, List<String>> map, Option option, List<String> newValue) {
		get(map, option).clear();
		set(map, option, newValue);
	}
}
