package oscrabble.dictionary.metainformationProviders;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.dictionary.DictionaryException;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

public class Wikitionary implements WordMetainformationProvider {
	public final static Logger LOGGER = LoggerFactory.getLogger(Wikitionary.class);

	private final String serverUrl;
	int width = 0;

	public Wikitionary(final String serverUrl) {
		this.serverUrl = serverUrl;
	}

	@Override
	public Iterable<String> getDefinitions(final String word) throws DictionaryException {
		// https://en.wiktionary.org/w/index.php?title=test&printable=yes
		try {
			String content = IOUtils.toString(
					new URL(serverUrl + "/w/index.php?title=" + word.toLowerCase() + "&printable=yes")
			);
			content = content.replaceAll("^<!DOCTYPE html>", "");
			content = content.replaceAll("^\\s*<html[^>]*>", "<html>");

			if (this.width > 1) {
				content = content.replaceFirst("<body ", "<body style='width: " + width + "px'");
			}

			return Collections.singleton(content);
		} catch (IOException e) {
			final String msg = "Cannot find " + word;
			LOGGER.error(msg, e);
			throw new DictionaryException(msg);
		}
	}

	public void setHtmlWidth(final int width) {
		this.width = width;
	}

}
