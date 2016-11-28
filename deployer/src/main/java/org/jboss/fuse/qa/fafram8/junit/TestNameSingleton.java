package org.jboss.fuse.qa.fafram8.junit;

import lombok.extern.slf4j.Slf4j;

/**
 * Provides test name to archiver when using FaframTestRunner.
 * Created by avano on 28.11.16.
 */
@Slf4j
public final class TestNameSingleton {
	private static TestNameSingleton instance;
	private static String testName;

	/**
	 * Constructor.
	 */
	private TestNameSingleton() {
	}

	/**
	 * Getter.
	 * @return instance
	 */
	public static TestNameSingleton getInstance() {
		if (instance == null) {
			instance = new TestNameSingleton();
		}
		return instance;
	}

	/**
	 * Setter.
	 * @param name test name
	 */
	public static void setTestName(String name) {
		testName = name;
	}

	/**
	 * Getter.
	 * @return test name
	 */
	public static String getTestName() {
		return testName;
	}
}
