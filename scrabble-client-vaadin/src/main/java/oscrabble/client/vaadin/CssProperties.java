package oscrabble.client.vaadin;

import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/**
 * Overloading of Map of strings for Cascading Style Sheets
 */
public class CssProperties extends LinkedHashMap<String, String> {
	@Override
	public String toString() {
		return this
				.entrySet()
				.stream()
				.map(e -> e.getKey() + ":" + e.getValue())
				.collect(Collectors.joining("; "));
	}
}
