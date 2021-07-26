package com.freya02.emojis.gen2;

import com.freya02.emojis.Emoji;
import com.freya02.emojis.EmojiStore;
import com.freya02.emojis.HttpUtils;
import com.freya02.emojis.Logging;
import com.freya02.ui.UILib;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import okhttp3.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.freya02.emojis.HttpUtils.CLIENT;

class EmojiDL2 {
	private static final Logger LOGGER = Logging.getLogger();

	private static final Path EMOJIS_JSON_PATH = Path.of("src/main/resources/com/freya02/emojis/emojis_test.json");
	private static final Path SHORTCODES_JSON_PATH = Path.of("data_cache/DiscordShortcodes.json");
	private static final Path CONFIG_PATH = Path.of("Config.json");

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static final ExecutorService es = Executors.newFixedThreadPool(48);

	private final Config config;
	private final EmojiStore store;

	private Scroller scroller;

	private int emojiInsertCount = 0;

	private EmojiDL2(Config config) throws IOException {
		this.config = config;

		store = EmojiStore.load(EMOJIS_JSON_PATH);
	}

	public static void main(String[] args) {
		try {
			Config config = new Gson().fromJson(Files.readString(CONFIG_PATH), Config.class);

			LOGGER.warn("Token length: {}", config.getToken().length());
			LOGGER.warn("Guild ID: {}", config.getGuildId());
			LOGGER.warn("Channel ID: {}", config.getChannelId());
			LOGGER.warn("Make sure you have these information correct, as well as having all the strings in the specified channel");
			LOGGER.warn("\tYou have to use a debugger to correctly copy the strings in Discord, STARTING FROM THE STRING 0, before the program can retrieve them");
			LOGGER.warn("Please type OK to continue, SKIP to skip shortcodes gathering, SKIP ALL to also skip the unicode gathering, or something else to abort");
			final Scanner scanner = new Scanner(System.in);
			final String next = scanner.nextLine();

			final boolean skip = next.equals("SKIP");
			final boolean skip_all = next.equals("SKIP ALL");
			if (!next.equals("OK") && !skip && !skip_all) {
				System.exit(-42);

				return;
			}

			new EmojiDL2(config).run(skip || skip_all, skip_all);
		} catch (Exception e) {
			LOGGER.error("An exception occurred while getting emojis", e);

			System.exit(-1);
		}

		System.exit(0);
	}

	private void run(boolean skip, boolean skipAll) throws Exception {
		ArrayList<List<String>> emojis = null;

		if (skip) {
			if (Files.notExists(SHORTCODES_JSON_PATH)) {
				skip = false;
				LOGGER.warn("Could not skip shortcodes gathering as the shortcodes JSON doesn't exist at {}", SHORTCODES_JSON_PATH.toAbsolutePath());
			} else {
				//noinspection unchecked
				emojis = GSON.fromJson(Files.readString(SHORTCODES_JSON_PATH), ArrayList.class);
			}
		}

		if (!skip) {
			UILib.runAndWait(() -> {
				final Stage stage = new Stage();

				final WebView view = new WebView();
				final WebEngine engine = view.getEngine();
				VBox.setVgrow(view, Priority.ALWAYS);

				engine.load("https://discord.com/channels/@me");

				final Button button = new Button("Capture");
				button.setOnAction(evt -> {
					try {
						scroller = new Scroller(stage, engine);
						stage.addEventFilter(EventType.ROOT, Event::consume);
						scroller.start();
					} catch (IllegalStateException e) {
						LOGGER.error("Unable to start auto scrolling", e);
					}
				});

				final var root = new VBox(5, button, view);

				final Scene scene = new Scene(root);
				stage.setScene(scene);
				stage.showAndWait();
			});

			if (scroller != null) {
				emojis = new ArrayList<>(scroller.getShortcodes());

				Files.writeString(SHORTCODES_JSON_PATH, GSON.toJson(emojis));
			}
		}

		if (emojis != null) {
			if (skipAll) {
				retrieveUnicodes(emojis);
			} else {
				makeStrings(emojis);
			}
		}

		es.shutdown();
		es.awaitTermination(1, TimeUnit.DAYS);

		HttpUtils.shutdown();

		store.save(EMOJIS_JSON_PATH);
	}

	private void makeStrings(List<List<String>> emojis) throws Exception {
		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection") final List<String> strings = new ArrayList<>();
		final StringBuilder sb = new StringBuilder();
		for (List<String> shortcodes : emojis) {
			String shortcode = shortcodes.get(0) + ' ';

			if (sb.length() + shortcode.length() > 2048) {
				strings.add(sb.toString());
				sb.setLength(0);
			}

			sb.append(shortcode);
		}

		strings.add(sb.toString());

		final long start = System.currentTimeMillis();
		new Object(); //PUT YOUR BREAKPOINT HERE
		final long end = System.currentTimeMillis();

		if (end - start < 30) {
			LOGGER.error("You probably didn't copy the strings as the supposed breakpoint paused for less than 30 ms");

			return;
		}

		retrieveUnicodes(emojis);
	}

	private void retrieveUnicodes(List<List<String>> emojis) throws Exception {
		final Request request = new Request.Builder()
				.url("https://discord.com/api/v8/channels/" + config.getChannelId() + "/messages")
				.header("Authorization", "Bot " + config.getToken())
				.get()
				.build();

		try (Response response = CLIENT.newCall(request).execute()) {
			if (!response.isSuccessful())
				throw new IOException("Response code: " + response.code());

			final ResponseBody body = response.body();
			if (body == null)
				throw new IOException("No body");

			final String string = body.string();

			int index = 0;
			List<Map<String, ?>> messages = GSON.fromJson(string, List.class);
			for (int i = messages.size() - 1; i >= 0; i--) {
				final String msg = (String) messages.get(i).get("content");

				for (String unicode : msg.split(" ")) {
					final List<String> shortcodes = emojis.get(index++)
							.stream()
							.map(s -> s.substring(1, s.length() - 1))
							.collect(Collectors.toList());

					es.submit(() -> {
						try {
							store.getEmojis().add(new Emoji(getEmojiSubpage(unicode), unicode, shortcodes));

							LOGGER.debug("[{}/{}] Added {}", emojiInsertCount++, emojis.size(), shortcodes.get(0));
						} catch (Exception e) {
							LOGGER.error("Could not add emoji {}", unicode, e);
						}
					});
				}
			}
		}
	}

	private String getEmojiSubpage(String unicode) throws IOException {
		final Call call = CLIENT.newCall(new Request.Builder()
				.url("https://emojipedia.org/search/?q=" + URLEncoder.encode(unicode, StandardCharsets.UTF_8))
				.head()
				.build());

		try (Response response = call.execute()) {
			if (response.isSuccessful()) {
				final HttpUrl newUrl = response.networkResponse().request().url();
				if (!newUrl.equals(call.request().url())) {
					if (!newUrl.pathSegments().isEmpty()) {
						return newUrl.pathSegments().get(0);
					} else {
						throw new IllegalStateException(String.format("Got an unexpected URL '%s' for %s, HTTP code %s", newUrl, unicode, response.code()));
					}
				} else {
					throw new IllegalStateException(String.format("Got the same URL for %s, HTTP code %s", unicode, response.code()));
				}
			} else {
				throw new IllegalStateException(String.format("Got no successful response for %s, HTTP code %s", unicode, response.code()));
			}
		}
	}
}