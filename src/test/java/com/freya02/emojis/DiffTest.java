package com.freya02.emojis;

import com.vdurmont.emoji.EmojiManager;

public class DiffTest {
	public static void main(String[] args) throws Exception {
		System.out.println("Trying to find missing emojis from emoji-java");

		final EmojiStore emojiStore = EmojiStore.loadLocal();
		{
			outer:
			for (Emoji emoji : emojiStore.getEmojis()) {
				for (String shortcode : emoji.shortcodes()) {
					if (EmojiManager.getForAlias(shortcode) != null) {
						continue outer;
					}
				}

				System.err.println("Not found for shortcodes: " + emoji.shortcodes());
			}
		}

		System.out.println("Trying to find missing emojis from JEmojis");
		{
			outer:
			for (var emoji : EmojiManager.getAll()) {
				for (String shortcode : emoji.getAliases()) {
					if (Emojis.ofShortcode(shortcode) != null) {
						continue outer;

					}
				}

				System.err.println("Not found for shortcodes: " + emoji.getAliases());
			}
		}
	}
}
