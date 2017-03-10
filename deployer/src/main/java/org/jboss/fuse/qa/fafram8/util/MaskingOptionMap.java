package org.jboss.fuse.qa.fafram8.util;

import java.util.HashMap;
import java.util.List;

/**
 * Map that masks the passwords in its toString method.
 * Created by avano on 10.3.17.
 */
public class MaskingOptionMap extends HashMap<Option, List<String>> {
	@Override
	public String toString() {
		return PasswordUtils.maskMap(super.toString());
	}
}
