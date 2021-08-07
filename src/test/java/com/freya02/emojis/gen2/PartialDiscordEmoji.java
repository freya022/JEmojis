package com.freya02.emojis.gen2;

import java.util.List;

class PartialDiscordEmoji {
	private final List<String> shortcodes;
	private final boolean supportsFitzpatrick;

	PartialDiscordEmoji(List<String> shortcodes, boolean supportsFitzpatrick) {
		this.shortcodes = shortcodes;
		this.supportsFitzpatrick = supportsFitzpatrick;
	}

	public List<String> getShortcodes() {
		return shortcodes;
	}

	public boolean doesSupportFitzpatrick() {
		return supportsFitzpatrick;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		PartialDiscordEmoji that = (PartialDiscordEmoji) o;

		if (supportsFitzpatrick != that.supportsFitzpatrick) return false;
		return shortcodes.equals(that.shortcodes);
	}

	@Override
	public int hashCode() {
		int result = shortcodes.hashCode();
		result = 31 * result + (supportsFitzpatrick ? 1 : 0);
		return result;
	}
}
