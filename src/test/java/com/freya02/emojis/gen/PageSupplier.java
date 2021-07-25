package com.freya02.emojis.gen;

import java.io.IOException;

interface PageSupplier {
	String get(String url) throws IOException;
}
