package oscrabble.configuration;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Field;

/**
 * Sammlung von Parameters, die zusammen eine Konfiguration darstellen.
 * Jeder Parameter ist als public Feld (Field) mit der Annotation {@link Parameter} zu versehen.
 * Die Methode {@link #createPanel()} ermöglicht die Anzeige und Änderung der Parameter durch Swing.
 */
public abstract class Configuration
{
	public static final Logger LOGGER = Logger.getLogger(Configuration.class);
	private final Listener listener;
	private final PropertyChangeSupport changeListeners = new PropertyChangeSupport(this);

	private final static Icon trueIcon = new ImageIcon(Configuration.class.getResource("checkboxTrue.png"));
	private final static Icon falseIcon = new ImageIcon(Configuration.class.getResource("checkboxFalse.png"));

	public Configuration()
	{
		this.listener = new Listener()
		{
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					setFromSource(e.getSource());
				}
				catch (ConfigurationException configurationException)
				{
					LOGGER.error(configurationException, configurationException);
					throw new Error(configurationException);
				}
			}

			@Override
			public void stateChanged(final ChangeEvent e)
			{
				try
				{
					setFromSource(e.getSource());
				}
				catch (ConfigurationException configurationException)
				{
					LOGGER.error(configurationException, configurationException);
					throw new Error(configurationException);
				}
			}

			private void setFromSource(final Object source) throws ConfigurationException
			{
				if (!(source instanceof Component))
				{
					throw new IllegalArgumentException("Source: " + source);
				}

				final String sourceName = ((Component) source).getName();
				setValue(sourceName, getValue(((Component) source)));
				System.out.println(Configuration.this.toString());
			}
		};
	}

	private Object getValue(final Component inputComponent)
	{
		if (inputComponent == null)
		{
			throw new IllegalArgumentException("Null argument");
		}

		if (inputComponent instanceof JTextField)
		{
			return ((JTextField) inputComponent).getText();
		}
		else if (inputComponent instanceof JCheckBox)
		{
			return ((JCheckBox) inputComponent).isSelected();
		}
		else if (inputComponent instanceof JSpinner)
		{
			return ((JSpinner) inputComponent).getValue();
		}
		else if (inputComponent instanceof JComboBox)
		{
			return ((JComboBox) inputComponent).getSelectedItem();
		}
		throw new IllegalArgumentException("Cannot treat: " + inputComponent.getClass());
	}

	/**
	 * Erstellt ein Panel.
	 */
	public JPanel createPanel()
	{
		final JPanel panel = new JPanel();
		panel.removeAll();
		panel.setLayout(new GridLayout(0, 2));

		doOnParameters((field, annotation)->addField(panel, field, annotation));
		return panel;
	}

	/**
	 * Fügt zu einem Panel die Widgets für ein Parameter Feld hinzu.
	 */
	private void addField(final JPanel panel, final Field field, final Parameter annotation) throws IllegalAccessException
	{
		final Object value;
		value = field.get(this);
		final JLabel label = new JLabel(annotation.label());
		label.setToolTipText(annotation.description().isEmpty() ? annotation.label() : annotation.description());
		panel.add(label);
		final Class<?> type = field.getType();
		final Component paramComponent;
		final PropertyChangeListener listener;
		if (String.class.equals(type))
		{
			paramComponent = new JTextField((String) value);
			((JTextField) paramComponent).addActionListener(this.listener);  // todo: reicht nicht: Fall berücksichtigen, wo man enter nicht gedrückt hat.
			listener = evt -> ((JTextField) paramComponent).setText(((String) evt.getNewValue()));
		}
		else if (type.isEnum())
		{
			final JComboBox<Object> cb = new JComboBox<>(type.getEnumConstants());
			cb.setSelectedItem(value);
			paramComponent = cb;
			cb.addActionListener(this.listener);
			listener = evt -> cb.setSelectedItem(evt.getNewValue());
		}
		else if (boolean.class.equals(type))
		{
			final JCheckBox cb = new JCheckBox((String) null, (Boolean) value);
			paramComponent = cb;
			cb.setIcon(falseIcon);
			cb.setSelectedIcon(trueIcon);
			cb.addActionListener(this.listener);
			listener = evt -> cb.setSelected(((Boolean) evt.getNewValue()));
		}
		else if (int.class.equals(type))
		{
			paramComponent = new JSpinner(new SpinnerNumberModel((int) value, 0, Integer.MAX_VALUE, 1));
			((JSpinner) paramComponent).addChangeListener(this.listener);
			listener = evt -> ((JSpinner) paramComponent).setValue(((Integer) evt.getNewValue()));
		}
		else
		{
			throw new IllegalArgumentException("Cannot treat type " + type);
		}
		final String fieldName = field.getName();
		paramComponent.setName(fieldName);
		this.changeListeners.addPropertyChangeListener(fieldName, listener);
		panel.add(paramComponent);
	}

	private abstract static class Listener implements ActionListener, ChangeListener
	{
	}

	public void setValue(final String fieldName, final Object newValue) throws ConfigurationException
	{
		try
		{
			final Field field = this.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			final Object oldValue = field.get(this);
			if (newValue == oldValue)
			{
				return;
			}
			field.set(this, newValue);
			this.changeListeners.fireIndexedPropertyChange(fieldName, -1, oldValue, newValue);
		}
		catch (final Throwable cause)
		{
			throw new ConfigurationException("Cannot set " + fieldName + " to " + newValue, cause);
		}
	}

	HierarchicalConfiguration<ImmutableNode> export()
	{
		final XMLConfiguration exportConfig = new XMLConfiguration();
		doOnParameters((field, anno) -> exportConfig.setProperty(field.getName(), field.get(this)));
		return exportConfig;
	}

	void read(HierarchicalConfiguration<ImmutableNode> source)
	{
		doOnParameters((field, anno) -> field.set(this, source.get(field.getClass(), field.getName())));
	}

	/**
	 * Führt eine Aktion auf alle Felder aus, die mit der Annotation  {@code Parameter} versehen sind.
	 * @param function die Funktion.
	 */
	private void doOnParameters(final FieldConsumer function)
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
	 * Exception
	 */
	public static class ConfigurationException extends Error
	{
		ConfigurationException(final String msg, final Throwable cause)
		{
			super(msg, cause);
		}
	}
}
