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
	private final Configuration configuration;

	private final static Icon trueIcon = new ImageIcon(ConfigurationPanel.class.getResource("checkboxTrue.png"));
	private final static Icon falseIcon = new ImageIcon(ConfigurationPanel.class.getResource("checkboxFalse.png"));
	private final Listener listener;

	public ConfigurationPanel(final Configuration configuration)
	{
		super();
		this.configuration = configuration;
		setLayout(new GridLayout(0, 2));

		this.listener = new Listener()
		{
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				setFromSource(e.getSource());
			}

			@Override
			public void stateChanged(final ChangeEvent e)
			{
				setFromSource(e.getSource());
			}

			private void setFromSource(final Object source)
			{
				if (!(source instanceof Component))
				{
					throw new IllegalArgumentException("Source: " + source);
				}

				final String sourceName = ((Component) source).getName();
				configuration.setValue(sourceName, getValue(((Component) source)));
			}
		};

		this.configuration.doOnParameters((field, annotation)->addField(field, annotation));
	}

	/**
	 * Extract from a component the selected value.
	 * @param source component to extract the value of.
	 * @return the found value.
	 */
	private Object getValue(final Component source)
	{
		if (source == null)
		{
			throw new IllegalArgumentException("Null argument");
		}

		if (source instanceof JTextField)
		{
			return ((JTextField) source).getText();
		}
		else if (source instanceof JCheckBox)
		{
			return ((JCheckBox) source).isSelected();
		}
		else if (source instanceof JSpinner)
		{
			return ((JSpinner) source).getValue();
		}
		else if (source instanceof JComboBox)
		{
			return ((JComboBox) source).getSelectedItem();
		}
		throw new IllegalArgumentException("Cannot treat: " + source.getClass());
	}

	/**
	 * Fügt zu einem Panel die Widgets für ein Parameter Feld hinzu.
	 */
	private void addField(final Field field, final Parameter annotation) throws IllegalAccessException
	{
		final Object value;
		value = field.get(this.configuration);
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
		this.configuration.changeListeners.addPropertyChangeListener(fieldName, listener);
		add(paramComponent);
	}

	/**
	 * Listener
	 */
	private abstract static class Listener implements ActionListener, ChangeListener
	{
	}



}
