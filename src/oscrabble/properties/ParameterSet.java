package oscrabble.properties;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Set of parameters and possibility to represent them in a Swing form.
 */
public class ParameterSet
{
	private final Set<Parameter> parameters = new LinkedHashSet<>();

	private final static Icon trueIcon = new ImageIcon(ParameterSet.class.getResource("checkboxTrue.png"));
	private final static Icon falseIcon = new ImageIcon(ParameterSet.class.getResource("checkboxFalse.png"));

	/**
	 * Create the set.
	 * @param parameters initally present parameters.
	 */
	public ParameterSet(final Parameter ... parameters)
	{
		for (final Parameter p : parameters)
		{
			add(p);
		}
	}

	/**
	 * Add a parameter in the set.
	 * @param parameter parameter to add
	 */
	public void add(final Parameter parameter)
	{
		this.parameters.add(parameter);
	}

	/**
	 * Create a panel to display and change the parameters. The elements of the panel automatically updates when the value of
	 * a parameter changes.
	 *
	 * @return the new created panel.
	 */
	public JPanel createPanel()
	{
		final JPanel panel = new JPanel();
		for (final Parameter opt : this.parameters)
		{
			final JCheckBox cb;
//			if (opt.hasArg())  // TODO
//			{
//				cb = new JCheckBox(opt.getValue());
//				((JCheckBox) cb).addActionListener(new ActionListener()
//				{
//					@Override
//					public void actionPerformed(final ActionEvent e)
//					{
//						opt.set
//					}
//				});
//			}
//			else
//			{
			cb = new JCheckBox(opt.name);
			cb.setSelectedIcon(trueIcon);
			cb.setIcon(falseIcon);
			cb.setSelected(((Parameter.Boolean) opt).getValue());
			cb.addActionListener(e -> opt.setValue(cb.isSelected()));
			opt.addListener(source -> cb.setSelected(((Parameter.Boolean) opt).getValue()));
//			}


			panel.add(cb);
			cb.setToolTipText(opt.description);
		}
		panel.setLayout(new GridLayout(0, 1));
		return panel;
	}

	Properties asProperties()
	{
		final Properties properties = new Properties();
		this.parameters.forEach(
				p -> properties.put(p.name, p.getValue())
		);
		return properties;
	}
}
