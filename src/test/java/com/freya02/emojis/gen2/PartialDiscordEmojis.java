package com.freya02.emojis.gen2;

import java.util.*;
import java.util.function.Consumer;

public class PartialDiscordEmojis implements Iterable<PartialDiscordEmoji> {
	private final List<PartialDiscordEmoji> emojis = new ArrayList<>();

	public PartialDiscordEmojis(Set<PartialDiscordEmoji> discordEmojis) {
		emojis.addAll(discordEmojis);
	}

	public List<PartialDiscordEmoji> getEmojis() {
		return emojis;
	}

	public int size() {return emojis.size();}

	public Iterator<PartialDiscordEmoji> iterator() {return emojis.iterator();}

	public boolean add(PartialDiscordEmoji partialDiscordEmoji) {return emojis.add(partialDiscordEmoji);}

	public PartialDiscordEmoji get(int index) {return emojis.get(index);}

	public Spliterator<PartialDiscordEmoji> spliterator() {return emojis.spliterator();}

	public void forEach(Consumer<? super PartialDiscordEmoji> action) {emojis.forEach(action);}
}
