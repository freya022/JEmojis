package com.freya02.emojis.utils;

import javafx.application.Platform;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class UILib {
	/** Posts a callable in {@linkplain Platform#runLater(Runnable)} and then waits for its completion.<br>
	 * <i> Info : This method does not initialize the FX thread </i> 
	 *
	 * @param callable The Callable to call
	 * @throws Exception If the callable threw an exception
	 * @throws IllegalStateException If this was called on the JFX thread
	 */
	public static <T> T runAndWait(Callable<T> callable) throws Exception {
		if (Platform.isFxApplicationThread()) {
			return callable.call();
		}

		//Checks if JFX is dead
		FXUtils.requireFXThreadAlive();

		Thread waitingThread = Thread.currentThread();
		class Holder<T2> {
			private Exception ex;
			private T2 result;
			private void done() { waitingThread.interrupt(); }
		}

		Holder<T> obs = new Holder<>();

		Platform.runLater(() -> {
			try {
				obs.result = callable.call();
			} catch (Exception e) {
				obs.ex = e;
			} finally {
				obs.done();
			}
		});

		try {
			Thread.sleep(Integer.MAX_VALUE);
		} catch (InterruptedException ignored) {
			//expected thing, not an error
		}

		if (obs.ex != null) {
			throw obs.ex;
		}

		return obs.result;
	}

	/**Posts a runnable in {@linkplain Platform#runLater(Runnable)} and then waits for its completion. <br>
	 * <i> Info : This method does not initialize the FX thread </i> 
	 *
	 * @param runnable The runnable to call
	 * @throws IllegalStateException If this was called on the JFX thread
	 */
	public static void runAndWait(Runnable runnable) {
		try {
			runAndWait(Executors.callable(runnable));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
