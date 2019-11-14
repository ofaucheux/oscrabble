package oscrabble.configuration;

import org.apache.log4j.Logger;

import java.beans.PropertyChangeSupport;
import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * Sammlung von Parameters, die zusammen eine Konfiguration darstellen.
 * Jeder Parameter ist als public Feld (Field) mit der Annotation {@link Parameter} zu versehen.
 **/
public interface Configurable
{

	/** listener to inform after a parameter has been changed */
	PropertyChangeSupport changeListeners = new PropertyChangeSupport(new Object());

	/** values as they where values as {@code #firePropertiesChange} was last called. Should be private. */
	HashMap<String, Object> oldValues = new HashMap<>();

	/** inform all listeners values have been changed */
	default void firePropertiesChange()
	{

		doOnParameters(
				(f, a) ->
				{
					final String property = f.getName();
					final Object newValue = f.get(this);
					final Object oldValue = this.oldValues.get(property);
//					if (!this.oldValues.containsKey(property)
//							|| (oldValue == null && newValue != null)
//							|| (oldValue != null && !oldValue.equals(newValue)))
//					{
						changeListeners.firePropertyChange(property, oldValue, newValue);
//					}
				}
		);
	}

	/**
	 * Führt eine Aktion auf alle Felder aus, die mit der Annotation  {@code Parameter} versehen sind.
	 * @param function die Funktion.
	 */
	default void doOnParameters(final FieldConsumer function)
	{
		Logger LOGGER = Logger.getLogger(Configurable.class);
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
					final ConfigurationPanel.ConfigurationException ex = new ConfigurationPanel.ConfigurationException("Error treating field " + field.getName(), e);
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

}
