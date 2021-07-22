package com.freya02.emojis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.freya02.emojis.Utils.WALKER;

public class Logging {
	public static Logger getLogger() {
		return LoggerFactory.getLogger(WALKER.getCallerClass());
	}

	public static Logger getLogger(Object obj) {
		return LoggerFactory.getLogger(obj.getClass());
	}
}
