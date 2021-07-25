package com.freya02.emojis.gen;

import com.freya02.emojis.Emoji;
import com.freya02.emojis.EmojiStore;
import com.freya02.emojis.Logging;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class EmojiDL {
	private static final Logger LOGGER = Logging.getLogger();

	private static final Path EMOJIS_JSON_PATH = Path.of("src/main/resources/com/freya02/emojis/emojis.json");

	private static final Pattern EMOJI_TABLE_PATTERN = Pattern.compile("<ul class=\"emoji-grid\">(\\X+?)</ul>");
	private static final Pattern EMOJI_LINE_PATTERN = Pattern.compile("<li(?> class=\"lazyparent\")?>(\\X+?)</li>");
	private static final Pattern EMOJI_SUBPAGE_PATTERN = Pattern.compile(
			"<a.*?href=\"/twitter/.*?/(.*?)/?\">\\X*?</a>"
	);

	private static final Pattern UNICODE_AND_NAME_PATTERN = Pattern.compile("<h1><span class=\"emoji\">(.*?)</span> (.*)</h1>");
	private static final Pattern SHORTCODE_PATTERN = Pattern.compile("<li><span class=\"shortcode\">:(.*?):</span>(\\X*?)</li>");

	private final Object LOCK = new Object();

	private final ExecutorService es = Executors.newFixedThreadPool(24);

	private final Cache cache;
	private final EmojiStore emojis;
	private final String tableName;
	private int skipped;

	public EmojiDL(String tableName) throws IOException {
		this.tableName = tableName;
		this.cache = new Cache(true);

		this.emojis = EmojiStore.load(EMOJIS_JSON_PATH);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				cache.close();

				emojis.save(EMOJIS_JSON_PATH);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}));
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		new EmojiDL("twemoji-13.1").start();
	}

	private void start() throws IOException, InterruptedException {
		final String twemojiTable = getTwemojiTable();

		final Matcher emojiLineMatcher = EMOJI_LINE_PATTERN.matcher(twemojiTable);
		while (emojiLineMatcher.find()) {
			final String emojiLine = emojiLineMatcher.group(1);
			final Matcher subpageMatcher = EMOJI_SUBPAGE_PATTERN.matcher(emojiLine);

			if (subpageMatcher.find()) {
				final String subpage = subpageMatcher.group(1);

				es.submit(() -> {
					try {
						final String url = "https://emojipedia.org/" + subpage;

						final String emojiPage = getEmojiPage(url);

						final Matcher nameMatcher = UNICODE_AND_NAME_PATTERN.matcher(emojiPage);
						if (!nameMatcher.find()) {
							throw new IllegalArgumentException("No name");
						}

						final String unicode = nameMatcher.group(1);
						final String name = nameMatcher.group(2);

						final String s = emojiPage.replace("&nbsp;", " ");
						if (s.contains("has not been Recommended For General Interchange")) {
							LOGGER.warn("No emoji version for {} at {} (2)", name, url);

							synchronized (LOCK) {
								skipped++;
							}

							return;
						}

						final Set<String> shortcodes = new HashSet<>();
						final Matcher shortcodeMatcher = SHORTCODE_PATTERN.matcher(emojiPage);
						while (shortcodeMatcher.find()) {
							final String shortcode = shortcodeMatcher.group(1);
							final String social = shortcodeMatcher.group(2);

							if (shortcode.startsWith("flag-")) {
								final String flag = shortcode.replace('-', '_');
								shortcodes.clear(); //Flag shortcode should be unique
								shortcodes.add(flag);
								LOGGER.info("Added flag {}", flag);

								break;
							}

							//Trying to adhere more to discord shortcode naming scheme,
							// comparing example such as "cow" and "cow head" reveals that it uses either Slack or Github shortcodes,
							// But i'd go with Github as https://emojipedia.org/star-struck/ shows github using underscores instead of dashes (like Slack)
							if (!social.contains("Slack") && !social.contains("Github")) {
								LOGGER.warn("Skipped shortcode {} in {}", shortcode, url);
								continue;
							}

							if (shortcode.contains("-") && !shortcode.equals("-1")) {
								LOGGER.debug("Replacing - by _ in {}", shortcode);
								shortcodes.add(shortcode.replace('-', '_'));

								continue;
							}

							shortcodes.add(shortcode);
						}

						//Manually add regional indicator symbols, there are none on emojipedia
						if (name.startsWith("Regional Indicator Symbol Letter")) {
							shortcodes.add("regional_indicator_" + name.substring(name.length() - 1).toLowerCase());
						}

						if (shortcodes.isEmpty()) {
							final String discordName = subpage.toLowerCase().replaceAll("[^a-z]", "_");
							LOGGER.warn("No shortcodes for {}, added {} manually", name, discordName);

							shortcodes.add(discordName);
						}

						emojis.getEmojis().add(new Emoji(subpage, name, unicode, new ArrayList<>(shortcodes)));

						LOGGER.debug("Put {}", name);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			} else {
				LOGGER.error("Subpage link not found for {}", emojiLine);
			}
		}

		es.shutdown();
		es.awaitTermination(1, TimeUnit.DAYS);

		LOGGER.info("Skipped {} emojis", skipped);

		HttpUtils.shutdown();
	}

	private String getEmojiPage(String url) throws IOException {
		return cache.computeIfAbsent(url, HttpUtils::getPageBody);
	}

	private String getTwemojiTable() throws IOException {
		return cache.computeIfAbsent("https://emojipedia.org/twitter/" + tableName, url -> {
			String page = HttpUtils.getPageBody(url);

			final Matcher tablePattern = EMOJI_TABLE_PATTERN.matcher(page);
			if (!tablePattern.find()) {
				throw new IOException("No table found");
			}

			return tablePattern.group(1);
		});
	}
}