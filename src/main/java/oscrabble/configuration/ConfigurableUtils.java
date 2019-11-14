package oscrabble.configuration;

import org.apache.commons.configuration2.Configuration;

import java.lang.reflect.Field;

public class ConfigurableUtils
{
	public static void loadProperties(final Configurable destination, final Configuration source)
	{

		source.getKeys().forEachRemaining(
				k -> {
					final Object newValue = source.get(Object.class, k);
						setValue(destination, k, newValue);
				}
		);
	}

	public static void setValue(final Configurable configurable, final String propertyName, final Object newValue) throws ConfigurationPanel.ConfigurationException
	{
		try
		{
			final Field field = configurable.getClass().getDeclaredField(propertyName);
			field.setAccessible(true);
			final Object oldValue = field.get(configurable);
			if (newValue == oldValue)
			{
				return;
			}
			field.set(configurable, newValue);
			configurable.changeListeners.fireIndexedPropertyChange(propertyName, -1, oldValue, newValue);
		}
		catch (final Throwable cause)
		{
			throw new ConfigurationPanel.ConfigurationException("Cannot set " + propertyName + " to " + newValue, cause);

		}
	}
}
