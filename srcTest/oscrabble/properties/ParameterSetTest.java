package oscrabble.properties;

import javax.swing.*;
import java.awt.*;

/**
 * Test class
 */
class ParameterSetTest
{

	@org.junit.jupiter.api.Test
	void createPanel() throws InterruptedException
	{
		final ParameterSet options = new ParameterSet();
		final Parameter.Boolean jaune = new Parameter.Boolean("Jaune", "La couleur doit-elle être jaune?", true);
		options.add(jaune);
		options.add(new Parameter.Boolean("Remplacement", "Couleur de remplacement si le jaune manque", true));
		options.add(new Parameter.Boolean("Laqué", null, true));

		System.out.println(options.toString());

		final JFrame frame = new JFrame();
		frame.setLayout(new FlowLayout());
		frame.add(options.createPanel());
		frame.add(options.createPanel());

		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);

		while (frame.isDisplayable())
		{
			jaune.setValue(!jaune.getValue(), null);
			Thread.sleep(3000);
		}

		System.out.println(options.asProperties());
	}
}