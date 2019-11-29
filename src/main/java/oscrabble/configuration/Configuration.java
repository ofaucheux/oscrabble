package oscrabble.configuration;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.log4j.Logger;

import java.beans.PropertyChangeSupport;
import java.lang.reflect.Field;
import java.util.Properties;

/**
 * Sammlung von Parameters, die zusammen eine Konfiguration darstellen.
 * Jeder Parameter ist als public Feld (Field) mit der Annotation {@link Parameter} zu versehen.
 **/
public abstract class Configuration
{

	/** listener to inform after a parameter has been changed */
	PropertyChangeSupport changeListeners = new PropertyChangeSupport(new Object());

	/**
	 * Führt eine Aktion auf alle Felder aus, die mit der Annotation  {@code Parameter} versehen sind.
	 * @param function die Funktion.
	 */
	void doOnParameters(final FieldConsumer function)
	{
		Logger LOGGER = Logger.getLogger(Configuration.class);
		for (final Field field : this.getClass().getDeclaredFields())
		{
			final Parameter annotation = field.getAnnotation(Parameter.class);
			if (annotation != null)
			{
				field.setAccessible(true);
				try
				{
					function.accept(field, annotation);
				}
				catch (final Throwable e)
				{
					final ConfigurationException ex = new ConfigurationException("Error treating field " + field.getName(), e);
					LOGGER.error(ex, ex);
					throw ex;
				}
			}
		}
	}

	@FunctionalInterface
	interface FieldConsumer
	{
		void accept(final Field field, final Parameter annotation) throws IllegalAccessException;
	}

	public void setValue(final String propertyName, final Object newValue) throws ConfigurationException
	{
		try
		{
			final Field field = FieldUtils.getDeclaredField(getClass(), propertyName, true);
			field.setAccessible(true);
			final Object oldValue = field.get(this);
			if (newValue == oldValue)
			{
				return;
			}

			if (field.getType().equals(Range.class))
			{
				// we want to change the "value" field of the range object
				final Object range = field.get(this);
				final Field valueField = Range.class.getDeclaredField("value");
				valueField.setAccessible(true);
				valueField.set(range, newValue);
			}
			else
			{
				field.set(this, newValue);
			}

			this.changeListeners.fireIndexedPropertyChange(propertyName, -1, oldValue, newValue);
		}
		catch (final Throwable cause)
		{
			throw new ConfigurationException("Cannot set " + propertyName + " to " + newValue, cause);
		}
	}

	/**
	 * Set the values from a properties source.
	 * @param properties the properties ot set.
	 */
	public void loadProperties(final Properties properties)
	{
		properties.stringPropertyNames().forEach(
				k -> {
					final Object newValue = properties.get(k);
					setValue(k, newValue);
				}
		);
	}

	/**
	 * Parameter object to represent an integer within a range. The representation will be a slider.
	 */
	public static class Range
	{
		final int lower;
		final int upper;

		private int value;

		/**
		 * Constructor
		 * @param lower
		 * @param upper
		 * @param value
		 */
		public Range(final int lower, final int upper, final Integer value)
		{
			assert lower < upper;
			this.lower = lower;
			this.upper = upper;
			this.value = value == null ? (lower + upper) / 2 : value;
			assert lower <= this.value;
			assert this.value <= upper;
		}

		public int getValue()
		{
			return this.value;
		}

	}

}


