package com.freya02.emojis;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class ActionImpl<T> implements Action<T> {
	private static final Logger LOGGER = Logging.getLogger();

	private static int threadNum = 0;
	private static final ExecutorService executor = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors(),
			60L, TimeUnit.SECONDS,
			new SynchronousQueue<>(), r -> {
				final Thread thread = new Thread(r);
				thread.setName("Queued action thread #" + threadNum++);

				return thread;
			});

	private final ActionSupplier<T> supplier;

	public ActionImpl(ActionSupplier<T> supplier) {
		this.supplier = supplier;
	}

	@Override
	public void queue(@Nullable Consumer<T> onSuccess, @Nullable Consumer<Throwable> onThrow) {
		executor.submit(() -> {
			try {
				final T result = complete();

				if (onSuccess != null) {
					onSuccess.accept(result);
				}
			} catch (Throwable e) {
				if (onThrow != null) {
					onThrow.accept(e);
				} else {
					if (LOGGER.isErrorEnabled()) {
						LOGGER.error("An exception occurred in a queued action", e);
					} else {
						e.printStackTrace();
					}
				}
			}
		});
	}

	@Override
	public <R> Action<R> flatMap(Function<T, R> flatmap) {
		return new ActionImpl<>(() -> flatmap.apply(complete()));
	}

	public T complete() {
		try {
			return supplier.get();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
}
