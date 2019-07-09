package oscrabble.properties;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A parameter consisting in a name, a description and a current value.
 * @param <T> Class of the value.
 */
public abstract class Parameter <T>
{
	private final Set<WeakReference<Listener>> listeners = new HashSet<>();

	final String name;
	final String description;
	private T defaultValue;
	private T value;

	Parameter(final String name, final String description, final T defaultValue)
	{
		this.name = name;
		this.description = description;
		this.defaultValue = defaultValue;
	}

	/**
	 * @return the actual value
	 */
	public T getValue()
	{
		return this.value == null ? this.defaultValue : this.value;
	}

	/**
	 * Replace the current value by a new one. If the value really changes, a signal is send to all listeners.
	 *
	 * @param value the new value
	 */
	void setValue(final T value)
	{
		final boolean change = !value.equals(this.value);
		this.value = value;
		if (change)
		{
			this.listeners.forEach(l -> l.get().valueChanged(this));
		}
	}

	/**
	 * Add a listener.
	 * @param listener listener to add
	 */
	public void addListener(final Listener listener)
	{
		this.listeners.add(new WeakReference<>(listener));
	}

	/**
	 * A parameter valued with {@code true} or {@code false}
	 */
	public static class Boolean extends Parameter<java.lang.Boolean>
	{
		public Boolean(final String name, final String description, final boolean defaultValue)
		{
			super(name, description, defaultValue);
		}
	}

	/**
	 * Interface for object to react on the change of the value of the parameter.
	 */
	public interface Listener
	{
		void valueChanged(Parameter source);
	}
}
