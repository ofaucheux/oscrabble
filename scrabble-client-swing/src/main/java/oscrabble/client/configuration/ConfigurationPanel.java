package oscrabble.client.configuration;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.optionalusertools.DateChangeListener;
import com.github.lgooddatepicker.zinternaltools.DateChangeEvent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Set;

/**
 * Panel für die Anzeige und Änderung der Parameter durch Swing.
 */
public final class ConfigurationPanel extends JPanel
{
	private final Configuration configuration;

	private final static Icon trueIcon = new ImageIcon(ConfigurationPanel.class.getResource("checkboxTrue.png"));
	private final static Icon falseIcon = new ImageIcon(ConfigurationPanel.class.getResource("checkboxFalse.png"));
	private final Listener listener;

	/**
	 * Create a panel with all fields visible.
	 * @param configuration configuration linked with the panel.
	 */
	public ConfigurationPanel(final Configuration configuration)
	{
		this(configuration, null, null);
	}

	/**
	 * Crete a panel.
	 * @param configuration configuration linked with the panel.
	 * @param readOnly		list of fields which to be displayed as read-only (see {@link JComponent#setEnabled(boolean)}, or {@code null}
	 * @param hidden        list of the fields to hide, or {@code null}
	 */
	public ConfigurationPanel(
			final Configuration configuration,
			final Set<String> readOnly,
			final Set<String> hidden)
	{
		super();
		this.configuration = configuration;
		setLayout(new GridLayout(0, 2));

		this.listener = new Listener()
		{
			@Override
			public void dateChanged(final DateChangeEvent event)
			{
				setFromSource(event.getSource());
			}

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

		this.configuration.doOnParameters((field, annotation) -> {
			final String fieldName = field.getName();
			if (hidden == null || !hidden.contains(fieldName))
			{
				final Component fieldComponent = addField(field, annotation);
				fieldComponent.setEnabled(readOnly == null || !readOnly.contains(fieldName));
			}
		});
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
			//noinspection rawtypes
			return ((JComboBox) source).getSelectedItem();
		}
		else if (source instanceof JSlider)
		{
			return ((JSlider) source).getValue();
		}
		else if (source instanceof DatePicker)
		{
			return ((DatePicker) source).getDate();
		}
		throw new IllegalArgumentException("Cannot treat: " + source.getClass());
	}

	/**
	 * Fügt zu einem Panel die Widgets für ein Parameter Feld hinzu.
	 * @return the created field
	 */
	private Component addField(final Field field, final Parameter annotation) throws IllegalAccessException
	{
		final Object value;
		value = field.get(this.configuration);
		final String labelText = i18n(annotation.label());
		final JLabel label = new JLabel(labelText);
		label.setToolTipText(annotation.description().isEmpty() ? labelText : i18n(annotation.description()));
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
			int lower;
			int upper;
			lower = annotation.lowerBound();
			upper = annotation.upperBound();
			if (lower == upper && upper == -1)
			{
				lower = 0;
				upper = Integer.MAX_VALUE;
			}

			if (annotation.isSlide())
			{
				paramComponent = new JSlider(
						lower,
						upper,
						(int) value
				);
				((JSlider) paramComponent).setPaintLabels(true);
				if (upper - lower < 100000)
				{
					((JSlider) paramComponent).setMajorTickSpacing((upper - lower) / 2);
				}
				((JSlider) paramComponent).addChangeListener(this.listener);
				listener = evt -> ((JSlider) paramComponent).setValue(((Integer) evt.getNewValue()));
			}
			else
			{
				paramComponent = new JSpinner(new SpinnerNumberModel((int) value, lower, upper, 1));
				((JSpinner) paramComponent).addChangeListener(this.listener);
				listener = evt -> ((JSpinner) paramComponent).setValue(((Integer) evt.getNewValue()));
			}
		}
		else if (type == LocalDate.class)
		{
			paramComponent = new DatePicker();
			((DatePicker) paramComponent).addDateChangeListener(this.listener);
			listener = evt -> ((DatePicker) paramComponent).setDate((LocalDate) evt.getNewValue());
		}
		else
		{
			throw new IllegalArgumentException("Cannot treat type " + type);
		}
		final String fieldName = field.getName();
		paramComponent.setName(fieldName);
		this.configuration.changeListeners.addPropertyChangeListener(fieldName, listener);
		add(paramComponent);
		return paramComponent;
	}

	/**
	 * Translate a text if starting with {@code #}.
	 * @param text text to translate
	 * @return the translation, or the text itself.
	 */
	private static String i18n(final String text)
	{
		if (text.startsWith("#"))
		{
			return Game.MESSAGES.getString(text.substring(1));
		}
		else
		{
			return text;
		}
	}

	/**
	 * Listener
	 */
	private abstract static class Listener implements ActionListener, ChangeListener, DateChangeListener
	{
	}



}
