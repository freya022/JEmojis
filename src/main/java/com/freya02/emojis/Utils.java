package com.freya02.emojis;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;

public class Utils {
	public static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

	@NotNull
	public static InputStream getResource(String name) {
		final Class<?> callerClass = WALKER.getCallerClass();

		final InputStream stream = callerClass.getResourceAsStream(name);
		if (stream == null) {
			throw new RuntimeException("Resource " + name + " not found in package " + callerClass.getPackageName());
		}

		return stream;
	}
}
