package com.freya02.emojis.gen2;

import com.freya02.emojis.Emoji;
import com.freya02.emojis.EmojiStore;
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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

class EmojiDL2 {
	private static final Logger LOGGER = Logging.getLogger();

	private static final Path EMOJIS_JSON_PATH = Path.of("src/main/resources/com/freya02/emojis/emojis_test.json");
	private static final Path SHORTCODES_JSON_PATH = Path.of("data_cache/DiscordShortcodes.json");
	private static final Path CONFIG_PATH = Path.of("Config.json");

	private static final OkHttpClient CLIENT = new OkHttpClient.Builder().build();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Config config;

	private Scroller scroller;

	private EmojiDL2(Config config) {
		this.config = config;
	}

	public static void main(String[] args) {
		try {
			Config config = new Gson().fromJson(Files.readString(CONFIG_PATH), Config.class);

			LOGGER.warn("Token length: {}", config.getToken().length());
			LOGGER.warn("Guild ID: {}", config.getGuildId());
			LOGGER.warn("Channel ID: {}", config.getChannelId());
			LOGGER.warn("Make sure you have these information correct, as well as having all the strings in the specified channel");
			LOGGER.warn("\tYou have to use a debugger to correctly copy the strings in Discord, STARTING FROM THE STRING 0, before the program can retrieve them");
			LOGGER.warn("Please type OK to continue, SKIP to skip shortcodes gathering, or something else to abort");
			final Scanner scanner = new Scanner(System.in);
			final String next = scanner.next();

			if (!next.equals("OK") && !next.equals("SKIP")) {
				System.exit(-42);

				return;
			}

			new EmojiDL2(config).run(next.equals("SKIP"));

			CLIENT.dispatcher().executorService().shutdownNow();
			CLIENT.connectionPool().evictAll();
		} catch (Exception e) {
			LOGGER.error("An exception occurred while getting emojis", e);

			System.exit(-1);
		}

		System.exit(0);
	}

	private void run(boolean skip) throws Exception {
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
			makeStrings(emojis);
		}
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
		final EmojiStore store = EmojiStore.load(EMOJIS_JSON_PATH);

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

					store.getEmojis().add(new Emoji("None", unicode, shortcodes));
				}
			}
		}

		store.save(EMOJIS_JSON_PATH);
	}
}