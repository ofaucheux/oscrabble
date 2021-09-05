package oscrabble.client.utils;

import java.util.Locale;

public class CaseUtils {
	public static String toSnakeCase(final String text) {
		String result = text.replaceAll("(\\p{Ll})(\\d)", "$1_$2");
		result = result.replaceAll("(\\p{Lu})", "_$1");
		result = result.charAt(0) == '_' ? result.substring(1) : result;
		result = result.toUpperCase(Locale.ROOT);
		return result;
	}
}
