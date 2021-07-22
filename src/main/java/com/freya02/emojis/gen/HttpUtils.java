package com.freya02.emojis.gen;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

class HttpUtils {
	public static final OkHttpClient client = new OkHttpClient.Builder().build();

	@NotNull
	static String getPageBody(String url) throws IOException {
		final Call call = client.newCall(new Request.Builder()
				.url(url)
				.build());

		String page;
		try (Response response = call.execute()) {
			if (response.isSuccessful() || response.isRedirect()) {
				page = response.body().string();
			} else {
				throw new IOException("Response code: " + response.code());
			}
		}

		return page;
	}

	static String getPageName(String url) {
		return url.substring(url.indexOf("://") + 3);
	}

	static void shutdown() {
		client.dispatcher().executorService().shutdownNow();
		client.connectionPool().evictAll();
	}
}
