package org.jboss.fuse.qa.fafram8.timer;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;

/**
 * Created by avano on 22.8.16.
 */
public final class TimerUtils {
	private static TimerUtils instance = null;

	private static Set<Timer> timerSet = new HashSet<>();

	private TimerUtils() {
	}

	public static TimerUtils getInstance() {
		if (instance == null) {
			instance = new TimerUtils();
		}
		return instance;
	}

	public static Timer getNewTimer(String name) {
		getInstance();
		final Timer t = new Timer(name);
		timerSet.add(t);
		return t;
	}

	public static void cleanTimers() {
		getInstance();
		for (Timer timer : timerSet) {
			timer.cancel();
		}
		timerSet.clear();
	}
}
