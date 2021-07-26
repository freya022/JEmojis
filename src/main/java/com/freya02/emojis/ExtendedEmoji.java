package com.freya02.emojis;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExtendedEmoji {
	private static final String BASE_URL = "https://emojipedia.org/";
	private static final Pattern NAME_PATTERN = Pattern.compile("<h1><span class=\"emoji\">.*?</span> (.*)</h1>");

	private final Emoji emoji;
	private final String name;

	private ExtendedEmoji(Emoji emoji, String name) {
		this.emoji = emoji;
		this.name = name;
	}

	public static ExtendedEmoji of(Emoji emoji) throws IOException {
		final String body = HttpUtils.getPageBody(BASE_URL + emoji.subpage());

		final Matcher nameMatcher = NAME_PATTERN.matcher(body);
		if (!nameMatcher.find()) {
			throw new IllegalArgumentException("Name of the emoji " + emoji.unicode() + " aka " + emoji.shortcodes() + " not found");
		}

		return new ExtendedEmoji(emoji, nameMatcher.group(1));
	}

	public Emoji getEmoji() {
		return emoji;
	}

	public String getName() {
		return name;
	}
}