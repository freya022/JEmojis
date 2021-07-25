package com.freya02.emojis;

import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Emojis {
	private static final EmojiStore store;

	static {
		try {
			store = EmojiStore.loadLocal();
		} catch (IOException e) {
			throw new RuntimeException("Unable to load emojis", e);
		}
	}

	public static Set<Emoji> getEmojis() {
		return Collections.unmodifiableSet(store.getEmojis());
	}

	public static Emoji ofUnicode(String unicode) {
		return UnicodeHolder.unicodeMap.get(unicode);
	}

	public static Emoji ofShortcode(String shortcode) {
		if (shortcode.charAt(0) == ':' && shortcode.charAt(shortcode.length() - 1) == ':') {
			return ShortcodeHolder.shortcodeMap.get(shortcode.substring(1, shortcode.length() - 1));
		} else {
			return ShortcodeHolder.shortcodeMap.get(shortcode);
		}
	}

	private static class UnicodeHolder { //Delay initialization, saves memory if not used
		private static final Logger LOGGER = Logging.getLogger();
		private static final Map<String, Emoji> unicodeMap = new HashMap<>();

		static {
			for (Emoji emoji : store.getEmojis()) {
				final Emoji old = unicodeMap.put(emoji.unicode(), emoji);
				if (old != null) {
					LOGGER.debug("Duplicate unicode: {} in https://emojipedia.org/{} and https://emojipedia.org/{}, might not be grave", emoji.unicode(), old.subpage(), emoji.subpage());
				}
			}

			LOGGER.debug("Loaded unicode map");
		}
	}

	private static class ShortcodeHolder { //Delay initialization, saves memory if not used
		private static final Logger LOGGER = Logging.getLogger();
		private static final Map<String, Emoji> shortcodeMap = new HashMap<>();

		static {
			for (Emoji emoji : store.getEmojis()) {
				for (String shortcode : emoji.shortcodes()) {
					final Emoji old = shortcodeMap.put(shortcode, emoji);
					if (old != null) {
						LOGGER.debug("Duplicate shortcode: {} in https://emojipedia.org/{} and https://emojipedia.org/{}, might not be grave", shortcode, old.subpage(), emoji.subpage());
					}
				}
			}

			LOGGER.debug("Loaded shortcode map");
		}
	}
}