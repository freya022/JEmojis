package com.freya02.emojis.tests;

import com.freya02.emojis.Emoji;
import com.freya02.emojis.EmojiParser;
import com.freya02.emojis.Emojis;
import com.freya02.emojis.TwemojiType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.reflect.Field;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Tests {
	private Pattern shortcodePattern;

	@BeforeAll
	public void setup() throws NoSuchFieldException, IllegalAccessException {
		final Field shortcodePatternField = EmojiParser.class.getDeclaredField("SHORTCODE_PATTERN");
		shortcodePatternField.setAccessible(true);

		shortcodePattern = (Pattern) shortcodePatternField.get(null);
	}

	@Test
	public void toUnicodeTest() {
		assertEquals("foo \uD83D\uDE02 bar", EmojiParser.toUnicode("foo :joy: bar"));
		assertEquals("foo \uD83D\uDE02 \uD83D\uDE20 bar", EmojiParser.toUnicode("foo :joy: :angry: bar"));
	}

	@Test
	public void ofShortcodeTest() {
		assertNotNull(Emojis.ofShortcode("joy"));
		assertNotNull(Emojis.ofShortcode("angry"));
		assertNotNull(Emojis.ofShortcode(":flag_fr:"));
		assertNotNull(Emojis.ofShortcode(":keycap_ten:"));
		assertNotNull(Emojis.ofShortcode(":cow:"));
		assertNotNull(Emojis.ofShortcode(":cow2:"));
	}

	@Test
	public void ofUnicodeTest() {
		assertNotNull(Emojis.ofUnicode("\uD83D\uDE02"));
		assertNotNull(Emojis.ofUnicode("\uD83D\uDE20"));
		assertNotNull(Emojis.ofUnicode("\uD83C\uDDEB\uD83C\uDDF7"));
		assertNotNull(Emojis.ofUnicode("\uD83D\uDD1F"));
		assertNotNull(Emojis.ofUnicode("\uD83D\uDC2E"));
		assertNotNull(Emojis.ofUnicode("\uD83D\uDC04"));
	}

	@Test
	public void checkShortcodesTest() {
		for (Emoji emoji : Emojis.getEmojis()) {
			for (String shortcode : emoji.shortcodes()) {
				final Matcher matcher = shortcodePattern.matcher(':' + shortcode + ':');

				assertTrue(matcher.matches(), () -> "Incorrect shortcode: " + shortcode);
			}
		}
	}

	@Test
	public void emojiMethodsTest() {
		final Emoji flag_bb = Emojis.ofShortcode("flag_bb");

		assertEquals("Flag: Barbados", flag_bb.name());
		assertEquals(List.of("\\uD83C", "\\uDDE7", "\\uD83C", "\\uDDE7"), flag_bb.getUTF16());
		assertEquals(List.of("U+1F1E7", "U+1F1E7"), flag_bb.getUnicodeCodepoints());
		assertEquals(List.of("1f1e7", "1f1e7"), flag_bb.getHexCodepoints());
		assertEquals("https://raw.githubusercontent.com/twitter/twemoji/master/assets/72x72/1f1e7-1f1e7.png", flag_bb.getTwemojiImageUrl(TwemojiType.X72));
		assertEquals("https://raw.githubusercontent.com/twitter/twemoji/master/assets/svg/1f1e7-1f1e7.svg", flag_bb.getTwemojiImageUrl(TwemojiType.SVG));
	}
}
