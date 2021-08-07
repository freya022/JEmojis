package com.freya02.emojis;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.freya02.emojis.TwemojiType.X72;

/**
 * Class containing basic info about an emoji such as their Unicode and shortcode representations with a few helper methods
 * <br>You can also see more info by using {@link #retrieveExtendedInfo()}
 *
 * @see #retrieveExtendedInfo()
 */
public final class Emoji {
	private final String subpage;
	private final String unicode;
	private final List<String> shortcodes; //Shortcodes does NOT have :
	private final boolean supportsFitzpatrick;

	public Emoji(@NotNull String subpage, @NotNull String unicode, @NotNull List<String> shortcodes, boolean supportsFitzpatrick) {
		this.subpage = subpage;
		this.unicode = unicode;
		this.shortcodes = shortcodes;
		this.supportsFitzpatrick = supportsFitzpatrick;

		for (String shortcode : shortcodes) {
			if (shortcode.contains(":")) {
				throw new IllegalArgumentException(": not allowed in shortcode " + shortcode);
			}
		}
	}

	/**
	 * Returns the Emojipedia.org subpage of this emoji (ex: https://emojipedia.org/<b>face-with-tears-of-joy</b>/)
	 *
	 * @return The subpage of Emojipedia.org for this emoji
	 */
	public String subpage() {
		return subpage;
	}

	/**
	 * Returns the Unicode for this emoji, such as <code>😂</code>
	 *
	 * @return The Unicode for this emoji
	 */
	public String unicode() {
		return unicode;
	}

	/**
	 * Returns the shortcodes for this emoji, such as <code>joy</code>
	 * <br><b>These shortcodes does not have : in them</b>
	 *
	 * @return A list of shortcodes for this emoji
	 */
	public List<String> shortcodes() {
		return shortcodes;
	}

	/**
	 * Returns whether this emoji supports fitzpatrick (skin color changes)
	 *
	 * @return <code>true</code> if the emoji can have a skin tone applied
	 */
	public boolean doesSupportFitzpatrick() {
		return supportsFitzpatrick;
	}

	/**
	 * Returns an Action to retrieve the extended info of this emoji
	 *
	 * @return An action to get the extended emoji info
	 */
	public Action<ExtendedEmoji> retrieveExtendedInfo() {
		return new ActionImpl<>(() -> ExtendedEmoji.of(this));
	}

	/**
	 * Returns a list of string representing the escaped UTF-16 representation of this emoji
	 * <br><code>😂</code> will return "[\u005CuD83D, \u005CuDE02]" for example
	 *
	 * @return The escaped UTF-16 strings for this emoji
	 */
	public List<String> getUTF16() {
		int[] codepoints = unicode.codePoints().toArray();

		List<String> list = new ArrayList<>();
		for (int codePoint : codepoints) {
			final char[] chars = Character.toChars(codePoint);
			for (char aChar : chars) {
				final String hex = Integer.toHexString(aChar).toUpperCase();
				if (hex.equals("FE0F"))
					continue; //Skip invisible codepoint which specifies that the preceding character should be displayed with emoji presentation

				list.add("\\u" + "0".repeat(4 - hex.length()) + hex);
			}
		}

		return list;
	}

	/**
	 * Returns a list of characters representing the unescaped UTF-16 representation of this emoji
	 * <br><code>😂</code> will return "[\uD83D, \uDE02]" for example
	 *
	 * @return The escaped UTF-16 strings for this emoji
	 */
	public List<Character> getUTF16Unescaped() {
		int[] codepoints = unicode.codePoints().toArray();

		List<Character> list = new ArrayList<>();
		for (int codePoint : codepoints) {
			final char[] chars = Character.toChars(codePoint);
			for (char aChar : chars) {
				if (aChar == 0xFE0F)
					continue; //Skip invisible codepoint which specifies that the preceding character should be displayed with emoji presentation

				list.add(aChar);
			}
		}

		return list;
	}

	/**
	 * Returns the codepoints as a <code>U+Codepoint</code> representation
	 * <br><code>😂</code> will return "[U+1F602]" for example
	 * <br><b>The codepoints will be in hexadecimal and full uppercase</b>
	 *
	 * @return The unicode codepoints representation
	 */
	public List<String> getUnicodeCodepoints() {
		return unicode.codePoints()
				.mapToObj(codepoint -> "U+" + Integer.toHexString(codepoint).toUpperCase())
				.collect(Collectors.toList());
	}

	/**
	 * Returns the codepoints as a <code>Codepoint</code> representation
	 * <br><code>😂</code> will return "[1f602]" for example
	 * <br><b>The codepoints will be in hexadecimal and full lowercase</b>
	 *
	 * @return The unicode codepoints representation
	 */
	public List<String> getHexCodepoints() {
		return unicode.codePoints()
				.mapToObj(Integer::toHexString)
				.collect(Collectors.toList());
	}

	/**
	 * Returns the Twemoji asset URL for this emoji
	 *
	 * @param type The type of the image you want, 72x72 or SVG
	 * @return The Twemoji assert URL of the specified type
	 */
	public String getTwemojiImageUrl(TwemojiType type) {
		final String folder = type == X72 ? "72x72" : "svg";
		final String extension = type == X72 ? ".png" : ".svg";

		return "https://raw.githubusercontent.com/twitter/twemoji/master/assets/" + folder + "/" + String.join("-", getHexCodepoints()) + extension;
	}

	/**
	 * <b>You need to install dependencies in order to use this method, see {@link EmojiRenderer}</b>
	 * <p>
	 * Creates a new {@link EmojiRenderer} for this emoji
	 * <br>This allows you to create an image of this emoji, in any size, without any quality loss
	 * <br><b>It is recommended you only use one renderer instance and only use it in one thread, in order to save resources.</b>
	 *
	 * @return A new {@link EmojiRenderer}
	 */
	public EmojiRenderer newRenderer() {
		try {
			return new EmojiRenderer().setEmoji(this);
		} catch (Exception e) {
			throw new RuntimeException("An exception occurred while making an EmojiRender, do you have JavaFX installed ?", e);
		}
	}

	/**
	 * @throws IllegalStateException | when the current unicode is empty
	 * @return New emoji object of current emoji + fritzpatrick type
	 */
	public String unicodeWithFritzpatrick(Fritzpatrick type) {
		StringBuilder sb = new StringBuilder(unicode);

		if (unicode.codePoints().count() == 0) throw new IllegalStateException("Emoji has no unicode representation.");
		int firstCodePoint = unicode.codePointAt(0);

		char[] chars = Character.toChars(firstCodePoint);
		sb.insert(chars.length > 1 ? 2 : 1, type.getUnicode());

		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Emoji emoji = (Emoji) o;

		if (supportsFitzpatrick != emoji.supportsFitzpatrick) return false;
		if (!subpage.equals(emoji.subpage)) return false;
		if (!unicode.equals(emoji.unicode)) return false;
		return shortcodes.equals(emoji.shortcodes);
	}

	@Override
	public int hashCode() {
		int result = subpage.hashCode();
		result = 31 * result + unicode.hashCode();
		result = 31 * result + shortcodes.hashCode();
		result = 31 * result + (supportsFitzpatrick ? 1 : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Emoji{" +
				"subpage='" + subpage + '\'' +
				", unicode='" + unicode + '\'' +
				", shortcodes=" + shortcodes +
				", supportsFitzpatrick=" + supportsFitzpatrick +
				'}';
	}
}