package oscrabble.configuration;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;

/**
 * Sammlung von Parameters, die zusammen eine Konfiguration darbilden.
 * Jeder Parameter ist als Feld (Field) mit der Annotation {@link Parameter} zu versehen.
 * Die Methode {@link #createPanel()} ermöglicht die Anzeige und Änderung der Parameter durch Swing.
 */
public abstract class Configuration
{
	private final Listener listener;

	public Configuration()
	{
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
				try
				{
					final Field field = Configuration.this.getClass().getField(sourceName);
					field.set(Configuration.this, getValue(((Component) source)));
				}
				catch (NoSuchFieldException ex)
				{
					throw new IllegalArgumentException("Parameter not found: " + sourceName, ex);
				}
				catch (IllegalAccessException ex)
				{
					throw new Error("Error setting " + sourceName, ex);
				}
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

		for (final Field field : this.getClass().getFields())
		{
			final Parameter annotation = field.getAnnotation(Parameter.class);
			if (annotation != null)
			{
				final Object value;
				try
				{
					value = field.get(this);
				}
				catch (IllegalAccessException e)
				{
					throw new Error("Problem", e);
				}
				panel.add(new JLabel(annotation.description()));
				final Class<?> type = field.getType();
				final Component paramComponent;
				if (String.class.equals(type))
				{
					paramComponent = new JTextField((String) value);
					((JTextField) paramComponent).addActionListener(this.listener);
				}
				else if (boolean.class.equals(type))
				{
					paramComponent = new JCheckBox((String) null, (Boolean) value);
					((JCheckBox) paramComponent).addActionListener(this.listener);
				}
				else if (int.class.equals(type))
				{
					paramComponent = new JSpinner(new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1));
					((JSpinner) paramComponent).addChangeListener(this.listener);
				}
				else
				{
					throw new IllegalArgumentException("Cannot treat type " + type);
				}
				paramComponent.setName(field.getName());
				panel.add(paramComponent);

			}
		}
		return panel;
	}

	private abstract static class Listener implements ActionListener, ChangeListener
	{
	}
}
