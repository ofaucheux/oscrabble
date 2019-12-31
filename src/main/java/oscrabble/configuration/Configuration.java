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

	private final static Logger LOGGER = Logger.getLogger(Configuration.class);

	/** listener to inform after a parameter has been changed */
	PropertyChangeSupport changeListeners = new PropertyChangeSupport(new Object());

	/**
	 * Führt eine Aktion auf alle Felder aus, die mit der Annotation  {@code Parameter} versehen sind.
	 * @param function die Funktion.
	 */
	void doOnParameters(final FieldConsumer function)
	{
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

	/**
	 * Set the value of a property and inform the listeners. The listeners are only informed if the value really has changed.
	 * @param propertyName name of the property
	 * @param newValue neu value.
	 * @throws ConfigurationException if any problem occurs.
	 */
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

			field.set(this, newValue);

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

}


