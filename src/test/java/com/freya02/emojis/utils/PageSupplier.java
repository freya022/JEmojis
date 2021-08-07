package com.freya02.emojis.utils;

import java.io.IOException;

public interface PageSupplier {
	String get(String url) throws IOException;
}
