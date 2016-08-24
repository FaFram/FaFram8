package org.jboss.fuse.qa.fafram8.timer;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;

/**
 * Singleton utils class for java.util.Timer.
 * Created by avano on 22.8.16.
 */
public final class TimerUtils {
	private static TimerUtils instance = null;

	private static Set<Timer> timerSet = new HashSet<>();

	/**
	 * Private constructor.
	 */
	private TimerUtils() {
	}

	/**
	 * Gets the instance.
	 * @return instance
	 */
	public static TimerUtils getInstance() {
		if (instance == null) {
			instance = new TimerUtils();
		}
		return instance;
	}

	/**
	 * Creates a new timer with given name, adds it to the timerSet and returns the new instance.
	 * @param name name
	 * @return timer instance
	 */
	public static Timer getNewTimer(String name) {
		getInstance();
		final Timer t = new Timer(name);
		timerSet.add(t);
		return t;
	}

	/**
	 * Cancels all timers in timerSet.
	 */
	public static void cancelTimers() {
		getInstance();
		for (Timer timer : timerSet) {
			timer.cancel();
		}
		timerSet.clear();
	}
}
