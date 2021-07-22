package com.freya02.emojis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public class EmojiStore {
	private static final Logger LOGGER = Logging.getLogger();
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	private final Map<String, Emoji> emojiMap = new HashMap<>();

	public static EmojiStore loadLocal() throws IOException {
		LOGGER.debug("Loading local emojis");

		final EmojiStore store;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(Utils.getResource("emojis.json")))) {
			store = GSON.fromJson(reader, EmojiStore.class);
		}

		LOGGER.debug("Loaded {} local emojis", store.getEmojiMap().size());

		return store;
	}

	public static EmojiStore load(Path path) throws IOException {
		LOGGER.debug("Loading emojis");

		if (Files.exists(path)) {
			final EmojiStore emojis = GSON.fromJson(Files.readString(path), EmojiStore.class);

			LOGGER.debug("Loaded emojis {} from JSON", emojis.emojiMap.size());

			return emojis;
		} else {
			LOGGER.debug("Loaded empty emojis data");

			return new EmojiStore();
		}
	}

	public Map<String, Emoji> getEmojiMap() {
		return emojiMap;
	}

	public void save(Path path) throws IOException {
		LOGGER.debug("Saving emojis");

		final String json = GSON.toJson(this);

		Files.writeString(path, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

		LOGGER.info("Saved {} emojis", emojiMap.size());
	}
}
