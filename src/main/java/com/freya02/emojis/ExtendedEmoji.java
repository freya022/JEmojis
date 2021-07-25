package com.freya02.emojis;

public final class ExtendedEmoji {
	private final Emoji emoji;
	private final String name;

	private ExtendedEmoji(Emoji emoji, String name) {
		this.emoji = emoji;
		this.name = name;
	}

	public static ExtendedEmoji of(Emoji emoji) {
		return new ExtendedEmoji(emoji, "name");
	}
}