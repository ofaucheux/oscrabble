package oscrabble.configuration;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Functions to work with {@link Properties} objects.
 */
public class PropertyUtils
{
	private PropertyUtils()
	{
		throw new AssertionError("Not instantiable");
	}

	/**
	 * Return a set of the properties which keys start with a prefix. The keys in the new set don't contain the prefix anymore.
	 *
	 * @param source original property set
	 * @param prefix searched prefix (without {@code .})
	 * @return the new property set.
	 */
	public static Properties getSubProperties(final Properties source, final String prefix)
	{
		final Properties result = new Properties();
		final Pattern keyPattern = Pattern.compile(Pattern.quote(prefix) + "\\.+(.*)");
		for (final String key : source.stringPropertyNames())
		{
			final Matcher m = keyPattern.matcher(key);
			if (m.matches())
			{
				result.setProperty(m.group(1), source.getProperty(key));
			}
		}
		return result;
	}

	/**
	 * Extend a set of properties with new ones, preceded from a prefix. Example: prefix {@code player} and property {@code name=Olivier} leads to
	 * property {@code player.name=Olivier}.<p/>
	 * Is the prefix {@code null} or empty, no prefix is prepend.
	 * @param mainProperties set of properties to extends
	 * @param subProperties properties to extend with
	 * @param prefix prefix to prepend (without {@code .})
	 */
	public static void addAsSubProperties (final Properties mainProperties, final Properties subProperties, String prefix)
	{
		if (prefix == null || prefix.trim().isEmpty())
		{
			prefix = "";
		}
		else
		{
			prefix = prefix.trim() + ".";
		}

		for (final String key : subProperties.stringPropertyNames())
		{
			final String newKey = prefix + key;
			if (mainProperties.containsKey(newKey))
			{
				throw new AssertionError("The main properties already contain the key " + newKey);
			}

			mainProperties.setProperty(newKey, subProperties.getProperty(key));
		}
	}
}
