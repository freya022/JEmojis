package com.freya02.emojis;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchEngine {
	private static final Pattern DDG_PATTERN = Pattern.compile("<a rel=\"nofollow\" class=\"result__a\" href=\"(.*)\">(.*)</a>");
	private static final String DDG_URL = "https://html.duckduckgo.com/html/?q=";

	public static List<SearchResult> findLinks(String request) throws IOException {
		final List<SearchResult> links = new ArrayList<>();

		final Call call = HttpUtils.CLIENT.newCall(new Request.Builder()
				.url(DDG_URL + URLEncoder.encode(request, StandardCharsets.UTF_8))
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36 Edg/89.0.774.68")
				.get()
				.build());

		try (Response response = call.execute()) {
			final String html = response.body().string();

			final Matcher matcher = DDG_PATTERN.matcher(html);
			while (matcher.find()) {
				final String url = matcher.group(1);
				final String title = matcher.group(2);

				links.add(new SearchResult(url, title));
			}

			return links;
		}
	}
}
