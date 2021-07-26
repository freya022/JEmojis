package com.freya02.emojis;

import java.util.Objects;

public final class SearchResult {
	private final String url;
	private final String title;

	public String url() { return url; }

	public String title() { return title; }

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (SearchResult) obj;
		return Objects.equals(this.url, that.url) &&
				Objects.equals(this.title, that.title);
	}

	@Override
	public int hashCode() {
		return Objects.hash(url, title);
	}

	@Override
	public String toString() {
		return "SearchResult[" +
				"url=" + url + ", " +
				"title=" + title + ']';
	}

	public SearchResult(String url, String title) {
		this.url = url;
		this.title = StringUtils.unescapeHtml3(title);
	}
}