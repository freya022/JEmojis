package com.freya02.emojis;

public interface ActionSupplier<T> {
	T get() throws Throwable;
}
