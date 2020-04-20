package oscrabble.client.configuration;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.log4j.Logger;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
					final String propertyValue = properties.getProperty(key);
					final Field field = getField(key);
					try
					{
						if (field != null)
						{
							final Class<?> fieldClass = field.getType();
							final Object newValue;
							// Primitives

							if ("null".equals(propertyValue))
							{
								newValue = null;
							}
							else if (fieldClass == String.class)
							{
								newValue = propertyValue;
							}
							else if (fieldClass == int.class)
							{
								newValue = Integer.valueOf(propertyValue);
							}
							else if (fieldClass == long.class)
							{
								newValue = Long.valueOf(propertyValue);
							}
							else if (fieldClass == float.class)
							{
								newValue = Float.valueOf(propertyValue);
							}
							else if (fieldClass == double.class)
							{
								newValue = Double.valueOf(propertyValue);
							}
							else if (fieldClass == boolean.class)
							{
								newValue = Boolean.valueOf(propertyValue);
							}
							else if (fieldClass == LocalDate.class)
							{
								newValue = LocalDate.parse(propertyValue, DateTimeFormatter.ISO_LOCAL_DATE);
							}
							else if (fieldClass.isEnum())
							{
								//noinspection unchecked,rawtypes
								newValue = Enum.valueOf( (Class<? extends Enum>) fieldClass, propertyValue);
							}
							else
							{
								// other classes: use the constructor
								final Constructor<?> constructor = ((Class<?>) fieldClass).getConstructor(String.class);
								newValue = constructor.newInstance(propertyValue);
							}

							setValue(key, newValue);
						}
					}
					catch (Throwable e)
					{
						errors.add("Cannot read " + key + "=" + propertyValue + " : " + e);
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
		final Properties properties = new Properties()
		{
			private boolean recurse;

			@Override
			public synchronized Set<Map.Entry<Object, Object>> entrySet()
			{
				if (this.recurse)
				{
					return super.entrySet();
				}
				else
				{
					this.recurse = true;
					final TreeMap<Object, Object> treeMap = new TreeMap<>(this);
					final Set<Map.Entry<Object, Object>> entries = treeMap.entrySet();
					this.recurse = false;
					return entries;
				}
			}
		};
		doOnParameters((field, annotation) -> {
			final Object value = field.get(this);
			final String stringValue = value instanceof Enum ? ((Enum<?>) value).name() : String.valueOf(value);
			properties.setProperty(field.getName(), stringValue);
		});
		return properties;
	}

}
