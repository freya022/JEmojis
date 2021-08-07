package com.freya02.emojis.gen2;

import com.freya02.emojis.Logging;
import com.freya02.emojis.utils.UILib;
import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Scroller {
	private static final Logger LOGGER = Logging.getLogger();

	private static final Pattern EMOJI_PATTERN = Pattern.compile("<span id=\"emoji-picker-list-row-item-(.*?)\" style=\"position: absolute; top: -999999px;\">(.*?)</span>");
	private static final Pattern SCROLLER_PATTERN = Pattern.compile("<div class=\"(scroller-.*? list-.*? thin-.*? scrollerBase-.*?)\".*?\"></div>");

//	private static final double FRAME_WAIT = 60.0;
//	private static final double DELAY_MS = 1000.0 / (60.0 / FRAME_WAIT);
	private static final int STEP = 90;

	//The user could have "most used" emojis that would cause duplicates, so we use a Set to deduplicate shortcode lists
	private final Set<PartialDiscordEmoji> discordEmojis = new LinkedHashSet<>();

	private final Matcher emojiMatcher = EMOJI_PATTERN.matcher("");

	private final JSObject scroller;
	private final Stage stage;
	private final JSObject pickerGrid;

	private int y = STEP;
	
	private boolean stop, stopped;

	Scroller(Stage stage, WebEngine engine) throws IllegalStateException {
		this.stage = stage;
		this.pickerGrid = (JSObject) engine.executeScript("document.getElementById(\"emoji-picker-grid\")");

		if (pickerGrid == null) {
			throw new IllegalStateException("Emoji picker grid not found");
		}

		final String scrollerClass = getScrollerClassNames();

		final JSObject scrollers = (JSObject) engine.executeScript("document.getElementsByClassName(\"" + scrollerClass + "\")");
		final int length = (int) scrollers.getMember("length");
		if (length > 1) {
			throw new IllegalStateException("Got more than one emoji scroller");
		} else if (length == 0) {
			throw new IllegalStateException("Got no emoji scroller");
		}

		this.scroller = (JSObject) scrollers.getSlot(0);

//		LOGGER.debug("{} frames of delay", FRAME_WAIT);
//		LOGGER.debug("{} ms delay", DELAY_MS);
	}

	//ok so basically, we search for emojis on every frame,
	// If no emojis are found, it means they are still loading
	// If emojis are found, it means they are loaded and we grab them, and scroll a bit
	// Repeat until scrolling doesn't work (old scrollTop == new scrollTop || old scrollTop + STEP > new scrollTop)
	// Could accelerate the process by caching resources with a URLConnection factor
	//      Would also allow to wait until no more items are getting downloaded before passing to the next step
	//      Or something like waiting for specific items
	//          Actually no, there could be other things being downloaded
	public void start() {
		new Thread(() -> {
			long start = System.nanoTime();

			try {
				while (!stopped) {
					UILib.runAndWait(() -> {
						final String gridHtml = (String) pickerGrid.getMember("innerHTML");

						int count = 0;
						emojiMatcher.reset(gridHtml);
						while (emojiMatcher.find()) {
							final boolean supportsFitzpatrick = emojiMatcher.group(1).contains("::skin-tone-");
							final List<String> shortcodes = Arrays.asList(emojiMatcher.group(2).split(" "));
							
							if (discordEmojis.add(new PartialDiscordEmoji(shortcodes, supportsFitzpatrick))) {
								count++;
							}
						}

						if (count != 0) { //If emoji found, they must have loaded
							LOGGER.info("Got {} new emojis", count);

							scroll();
						}
					});
				}
			} catch (Exception e) {
				LOGGER.error("An exception occurred while scrolling for emojis", e);
			} finally {
				long end = System.nanoTime();

				LOGGER.info("Completed emoji search in {} ms", String.format("%.3f", (end - start) / 1000000.0));

				Platform.runLater(stage::close);
			}
		}).start();
	}

	public Set<PartialDiscordEmoji> getDiscordEmojis() {
		return discordEmojis;
	}

	/**
	 * <code>true to continue scrolling</code>
	 */
	private void scroll() {
		if (stop) return;
		
		double oldScrollTop = (int) scroller.getMember("scrollTop");
		scroller.call("scrollTo", 0, y);
		double newScrollTop = (int) scroller.getMember("scrollTop");

		if (!stop && (oldScrollTop + STEP > newScrollTop || oldScrollTop == newScrollTop)) {
			stop = true;
			
			Executors.newScheduledThreadPool(1).schedule(() -> {
				final long shortcodeCount = discordEmojis.stream().mapToLong(de -> de.getShortcodes().size()).sum();
				LOGGER.info("Finished scrolling ! Got {} emojis for {} shortcodes", discordEmojis.size(), shortcodeCount);
				LOGGER.info("\t If the number of emojis does not correspond to what has been manually counted, it *may* mean the scroller didn't capture the last emojis, in which case you should up the scheduler delay");
				
				stopped = true;
			}, 1, TimeUnit.SECONDS);
		}

		y += STEP;
	}

	@NotNull
	private String getScrollerClassNames() {
		final String html = (String) pickerGrid.getMember("innerHTML");

		final Matcher scrollerMatcher = SCROLLER_PATTERN.matcher(html);
		if (!scrollerMatcher.find()) {
			throw new IllegalStateException("Scroller not found");
		}

		final String scrollerClass = scrollerMatcher.group(1);
		LOGGER.debug("Scroller found, class: '{}'", scrollerClass);

		return scrollerClass;
	}
}
