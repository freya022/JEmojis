package com.freya02.emojis.utils;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.List;

public class FXUtils {
	private static Thread fxThread;

	private static List<Thread> getAllThreads() {
		// Get current Thread Group
		ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
		ThreadGroup parentThreadGroup;
		while ((parentThreadGroup = threadGroup.getParent()) != null) {
			threadGroup = parentThreadGroup;
		}
		// List all active Threads
		final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		int nAllocated = threadMXBean.getThreadCount();
		int n;
		Thread[] threads;
		do {
			nAllocated *= 2;
			threads = new Thread[nAllocated];
			n = threadGroup.enumerate(threads, true);
		} while (n == nAllocated);
		threads = Arrays.copyOf(threads, n);
		return Arrays.asList(threads);
	}
	
	private static void setFxThread() {
		for (final Thread thread : getAllThreads()) {
			if (thread.getName().equals("JavaFX Application Thread")) { //Means JFX is still there
				fxThread = thread;
				break;
			}
		}
	}
	
	/**Checks if the JavaFX Application Thread has already been initialized and died <p>
	 *
	 * <b>Info :</b> JFX Thread can be kept alive by setting {@linkplain Platform#setImplicitExit(boolean)} to false. It could be closed by calling {@linkplain Platform#exit()}
	 *
	 * @return true if the JFX thread has died, false if not.
	 */
	public static boolean isFXThreadDead() {
		if (fxThread != null) {
			return !fxThread.isAlive();
		}
		
		try {
			PlatformImpl.startup(() -> {}, false);
			setFxThread();
			return false; //start up succeeded OR runnable has been passed to runLater, FX thread is alive
		} catch (IllegalStateException e) { //The only error possible
			if ("Platform.exit has been called".equals(e.getMessage())) {
				return true;
			}
		}

		//Check if JFX thread not dead
		if (Platform.isImplicitExit()) {
			for (final Thread thread : getAllThreads()) {
				if (thread.getName().equals("JavaFX Application Thread")) { //Means JFX is still there
					fxThread = thread;
					return false;
				}
			}
			return true; //Means JFX has not been yet found
		}
		
		return false; //Means JFX might not be dead as implicit exit is not
	}
	
	public static void requireFXThreadAlive() {
		if (isFXThreadDead()) {
			throw new IllegalStateException("JavaFX Application Thread has already been initialized but died");
		}
	}
}
