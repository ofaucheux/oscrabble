package oscrabble.configuration;

import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;

/**
 * Panel für die Anzeige und Änderung der Parameter durch Swing.
 */
public final class ConfigurationPanel extends JPanel
{
	public static final Logger LOGGER = Logger.getLogger(ConfigurationPanel.class);
	private final Configurable configurable;

	private final static Icon trueIcon = new ImageIcon(ConfigurationPanel.class.getResource("checkboxTrue.png"));
	private final static Icon falseIcon = new ImageIcon(ConfigurationPanel.class.getResource("checkboxFalse.png"));
	private final Listener listener;

	public ConfigurationPanel(final Configurable configurable)
	{
		super();
		this.configurable = configurable;
		setLayout(new GridLayout(0, 2));


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
				configurable.firePropertiesChange();;
			}
		};

		this.configurable.doOnParameters((field, annotation)->addField(field, annotation));
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
	 * Fügt zu einem Panel die Widgets für ein Parameter Feld hinzu.
	 */
	private void addField(final Field field, final Parameter annotation) throws IllegalAccessException
	{
		final Object value;
		value = field.get(this.configurable);
		final JLabel label = new JLabel(annotation.label());
		label.setToolTipText(annotation.description().isEmpty() ? annotation.label() : annotation.description());
		add(label);
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
		this.configurable.changeListeners.addPropertyChangeListener(fieldName, listener);
		add(paramComponent);
	}

	private abstract static class Listener implements ActionListener, ChangeListener
	{
	}

	private void setValue(final String fieldName, final Object newValue) throws ConfigurationException
	{
		ConfigurableUtils.setValue(this.configurable, fieldName, newValue);
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
