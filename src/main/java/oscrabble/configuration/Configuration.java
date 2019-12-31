package oscrabble.configuration;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.log4j.Logger;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Sammlung von Parameters, die zusammen eine Konfiguration darstellen.
 * Jeder Parameter ist als public Feld (Field) mit der Annotation {@link Parameter} zu versehen.
 **/
public abstract class Configuration
{

	private final static Logger LOGGER = Logger.getLogger(Configuration.class);

	/**
	 * listener to inform after a parameter has been changed
	 */
	PropertyChangeSupport changeListeners = new PropertyChangeSupport(new Object());


	/**
	 * Functions to create an object with known type from a string value. If the creation is possible, the value is returned.
	 * Elsewhere the return value is {@code null}.
	 */
	private final static List<BiFunction<Class<?>, String, Object>> INSTANTIATORS = Arrays.asList(
			(fieldClass, property) -> String.class.equals(fieldClass) ? property : null,
			(fieldClass, property) ->
			{
				try
				{
					final Method valueOfMethod = fieldClass.getMethod("valueOf", String.class);
					return valueOfMethod.invoke(null, property);
				}
				catch (NoSuchMethodException e)
				{
					return null;
				}
				catch (IllegalAccessException | InvocationTargetException e)
				{
					throw new Error(e);
				}
			},
			(fieldClass, property) ->
			{
				final Constructor<?> constructor;
				try
				{
					constructor = fieldClass.getConstructor(String.class);
					return constructor.newInstance(property);
				}
				catch (NoSuchMethodException e)
				{
					return null;
				}
				catch (IllegalAccessException | InstantiationException | InvocationTargetException e)
				{
					throw new Error(e);
				}
			}
	);


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

	/**
	 * Add a listener for change of any property.
	 * @param listener listener to add
	 */
	public void addChangeListener(final PropertyChangeListener listener)
	{
		this.changeListeners.addPropertyChangeListener(listener);
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
			final Field field = getField(propertyName);
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
	 * Get a field by its name.
	 *
	 * @param fieldName name of the field
	 * @return the field, {@code null} if no such one
	 */
	protected Field getField(final String fieldName)
	{
		return FieldUtils.getDeclaredField(getClass(), fieldName, true);
	}

	/**
	 * Set the values from a properties source.
	 * @param properties the properties ot set.
	 */
	public void loadProperties(final Properties properties)
	{

		final Set<String> errors = new HashSet<>();
		properties.stringPropertyNames().forEach(
				key -> {
					final String property = properties.getProperty(key);
					final Field field = getField(key);
					if (field != null)
					{
						Object newValue = null;
						final Class<?> type = field.getType();
						for (final BiFunction<Class<?>, String, Object> instantiator : INSTANTIATORS)
						{
							newValue = instantiator.apply(type, property);
							if (newValue != null)
							{
								break;
							}
						}
						if (newValue != null)
						{
							setValue(key, newValue);
						}
						else
						{
							errors.add("Cannot instantiate " + key + " with value " + property);
						}
					}
				}
		);

		if (!errors.isEmpty())
		{
			throw new ConfigurationException("Cannot read the properties", new Error(errors.size() + " errors: " + errors.toString()));
		}
	}

	/**
	 *
	 * @return the values of this configuration in form of properties.
	 */
	public Properties asProperties()
	{
		final Properties properties = new Properties();
		doOnParameters( (p,v) -> properties.setProperty(p.getName(), String.valueOf(p.get(this))));
		return properties;
	}

}
