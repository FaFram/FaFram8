package org.jboss.fuse.qa.fafram8.test.base;

/**
 * Fafram test base class. This class has common constants/methods useful for all the tests.
 * Created by avano on 5.1.16.
 */
public final class FaframTestBase {
	public static final String CURRENT_HTTP_URL = "http://download.eng.rdu2.redhat.com/brewroot/repos/jb-fuse-6.2-build/"
			+ "latest/maven/org/jboss/fuse/jboss-fuse-full/" + System.getProperty("fuse.version") + "/jboss-fuse-full-"
			+ System.getProperty("fuse.version") + ".zip";
	public static final String CURRENT_LOCAL_URL = "file:/qa/fuseqe-shared/fuse/jboss-fuse-full-6.2.1.redhat-084.zip";
	public static final String CURRENT_WIN_LOCAL_URL = "file:/cygdrive/c/storage/fuse/jboss-fuse-full-6.2.1.redhat-084.zip";

	/**
	 * Constructor.
	 */
	private FaframTestBase() {
	}

	/**
	 * Returns the url to the specified version.
	 *
	 * @param version version (e.g. 6.2.1-)
	 * @return url to the specified version
	 */
	public static String getVersion(String version) {
		return "http://download.eng.rdu2.redhat.com/brewroot/repos/jb-fuse-6.2-build/latest/maven/org/jboss/fuse/jboss-fuse-full/" + version
				+ "/jboss-fuse-full-" + version + ".zip";
	}
}
