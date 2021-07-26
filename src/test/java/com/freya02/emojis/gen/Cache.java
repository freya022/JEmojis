package com.freya02.emojis.gen;

import com.freya02.emojis.HttpUtils;
import com.freya02.emojis.Logging;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

class Cache implements Closeable {
	private static final Logger LOGGER = Logging.getLogger();

	private final Path cachePath;
	private final boolean zip;

	Cache(boolean zip) throws IOException {
		this.zip = zip;

		Path path = Path.of("data_cache");
		Files.createDirectories(path);

		if (!zip) {
			this.cachePath = path;
		} else {
			final Path zipPath = path.resolve("data_cache.zip");

			//https://docs.oracle.com/javase/7/docs/technotes/guides/io/fsp/zipfilesystemprovider.html
			final Map<String, String> env = Map.of("create", "true",
					"noCompression", "true");
			final URI uri = URI.create("jar:" + zipPath.toUri());

			this.cachePath = FileSystems.newFileSystem(uri, env).getPath("");
		}
	}

	String computeIfAbsent(String url, PageSupplier supplier) throws IOException {
		Path path = getUrlPath(url);

		if (Files.notExists(path)) {
			LOGGER.trace("Downloading {} at {}", url, path);

			String page = supplier.get(url);

			Files.createDirectories(path.getParent());
			Files.writeString(path, page, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

			return page;
		} else {
			LOGGER.trace("Reading {}", path);

			return Files.readString(path);
		}
	}

	Path getUrlPath(String url) {
		Path p = cachePath;

		for (String s : HttpUtils.getPageName(url).split("/")) {
			p = p.resolve(s);
		}

		return cachePath.getFileSystem().getPath(p.toAbsolutePath() + ".html");
	}

	@Override
	public void close() throws IOException {
		if (zip) {
			LOGGER.info("Closed ZIP cache");

			cachePath.getFileSystem().close();
		}
	}
}
